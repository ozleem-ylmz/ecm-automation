param(
    [string]$EcmRepo = "C:\Users\ozlemy\ECM",
    [string]$AutomationRepo = "C:\Users\ozlemy\IdeaProjects\ecm-automation",
    [string]$BaseRef = "",
    [string]$HeadRef = "",
    [switch]$FailOnMissingCoverage,
    [switch]$FailOnRemovedCoveredEndpoint,
    [switch]$NoFail
)

$ErrorActionPreference = "Stop"

$featureRoot = Join-Path $AutomationRepo "src\test\resources\features"
$outputDir  = Join-Path $AutomationRepo "target\change-detection"
$reportFile = Join-Path $outputDir "change-report-v3.1.txt"
$jsonFile   = Join-Path $outputDir "change-report-v3.1.json"

New-Item -ItemType Directory -Force $outputDir | Out-Null

function Normalize-RepoPath {
    param([string]$Path)
    if ($null -eq $Path) { return "" }
    return $Path.Trim().Trim('"').Replace("\", "/")
}

function Get-ModuleFromPath {
    param([string]$Path)

    $p = (Normalize-RepoPath $Path).ToLowerInvariant()

    if ($p -match "documents|document_copy") { return "Documents" }
    if ($p -match "folders|folder") { return "Folders" }
    if ($p -match "(^|/)auth|login") { return "Auth" }
    if ($p -match "rbac|roles|permissions") { return "RBAC" }
    if ($p -match "classes|class") { return "Classes" }
    if ($p -match "groups|group") { return "Groups" }
    if ($p -match "users|user") { return "Users" }
    if ($p -match "relationships") { return "Relationships" }
    if ($p -match "saved_searches|saved-searches") { return "Saved Searches" }
    if ($p -match "(^|/)tags|tags\.rs") { return "Tags" }
    if ($p -match "(^|/)labels|labels\.rs") { return "Labels" }
    if ($p -match "audit") { return "Audit" }
    if ($p -match "bulk") { return "Bulk" }
    if ($p -match "validation") { return "Validation" }
    if ($p -match "property_templates") { return "Property Templates" }
    if ($p -match "custom_fields") { return "Custom Fields" }
    if ($p -match "locks|checkout|checkin") { return "Locks" }
    if ($p -match "health") { return "Health" }
    if ($p -match "config") { return "Config" }

    if ($p -match "^frontend/src/pages/([^/]+)") {
        return "Frontend/$($Matches[1])"
    }

    if ($p -match "^frontend/") { return "Frontend" }

    # New OpenAPI path file: preserve its name so a completely new module is visible.
    if ($p -match "^src/api/rest/openapi/paths/([^/]+)\.rs$") {
        $name = $Matches[1] -replace "_", " "
        return (Get-Culture).TextInfo.ToTitleCase($name)
    }

    return "Other"
}

function Get-EndpointModule {
    param(
        [string]$EndpointPath,
        [string]$File
    )

    $p = (Normalize-RepoPath $EndpointPath).ToLowerInvariant()

    if ($p -match "^/v1/config(?:/|$)") { return "Config" }
    if ($p -match "^/v1/documents(?:/|$)") { return "Documents" }
    if ($p -match "^/v1/folders(?:/|$)") { return "Folders" }
    if ($p -match "^/v1/auth(?:/|$)") { return "Auth" }
    if ($p -match "^/v1/rbac(?:/|$)") { return "RBAC" }
    if ($p -match "^/v1/classes(?:/|$)") { return "Classes" }
    if ($p -match "^/v1/groups(?:/|$)") { return "Groups" }
    if ($p -match "^/v1/users(?:/|$)") { return "Users" }
    if ($p -match "^/v1/relationships(?:/|$)") { return "Relationships" }
    if ($p -match "^/v1/saved-searches(?:/|$)") { return "Saved Searches" }
    if ($p -match "^/v1/tags(?:/|$)") { return "Tags" }
    if ($p -match "^/v1/labels(?:/|$)") { return "Labels" }
    if ($p -match "^/v1/audit(?:/|$)") { return "Audit" }
    if ($p -match "^/v1/bulk(?:/|$)") { return "Bulk" }
    if ($p -match "^/v1/validation(?:/|$)") { return "Validation" }
    if ($p -match "^/v1/property-templates(?:/|$)") { return "Property Templates" }
    if ($p -match "^/v1/custom-fields(?:/|$)") { return "Custom Fields" }
    if ($p -match "^/v1/locks(?:/|$)") { return "Locks" }
    if ($p -match "^/health(?:/|$)") { return "Health" }

    return Get-ModuleFromPath $File
}
function Is-IgnoredFile {
    param([string]$Path)
    $p = Normalize-RepoPath $Path

    return (
        $p -match "\.zip$" -or
        $p -match "\.log$" -or
        $p -match "^target/" -or
        $p -match "^\.idea/" -or
        $p -match "^node_modules/" -or
        $p -match "^frontend/node_modules/"
    )
}

function Get-EndpointTag {
    param(
        [string]$Method,
        [string]$EndpointPath
    )

    $normalized = $EndpointPath.Trim()
    $normalized = $normalized -replace "[{}]", ""
    $normalized = $normalized -replace "[^A-Za-z0-9]+", "_"
    $normalized = $normalized.Trim("_")

    return "@endpoint_$($Method.ToUpperInvariant())_$normalized"
}
function Get-RuntimeEndpointCoverage {
    param(
        [string]$Method,
        [string]$EndpointPath
    )

    $runtimeCoverageFile = Join-Path $AutomationRepo "target\runtime-endpoint-coverage.csv"

    if (-not (Test-Path $runtimeCoverageFile -PathType Leaf)) {
        return @()
    }

    try {
        $rows = @(Import-Csv $runtimeCoverageFile -Encoding UTF8)

        return @(
            $rows |
                Where-Object {
                    $_.method -eq $Method -and
                    $_.endpoint -eq $EndpointPath
                } |
                Select-Object -ExpandProperty scenario -Unique
        )
    }
    catch {
        Write-Warning "Runtime endpoint coverage could not be read: $($_.Exception.Message)"
        return @()
    }
}

function Get-EndpointCoverage {
    param(
        [string]$Method,
        [string]$EndpointPath
    )

    $tag = Get-EndpointTag -Method $Method -EndpointPath $EndpointPath
    $features = @()
    $tagScenarios = @()

    if (Test-Path $featureRoot -PathType Container) {
        $featureFiles = @(Get-ChildItem $featureRoot -Filter "*.feature" -Recurse -File)

        foreach ($file in $featureFiles) {
            $fileLines = @(Get-Content $file.FullName -Encoding UTF8)
            $relative = $file.FullName.Replace($AutomationRepo + "\", "")

            for ($i = 0; $i -lt $fileLines.Count; $i++) {
                if ($fileLines[$i] -notmatch [regex]::Escape($tag)) {
                    continue
                }

                if ($features -notcontains $relative) {
                    $features += $relative
                }

                $scenarioName = $null
                $max = [Math]::Min($fileLines.Count - 1, $i + 8)

                for ($j = $i + 1; $j -le $max; $j++) {
                    if ($fileLines[$j] -match '^\s*Scenario(?: Outline)?:\s*(.+?)\s*$') {
                        $scenarioName = $Matches[1]
                        break
                    }

                    if (
                        $fileLines[$j] -match '^\s*Feature:' -or
                        ($fileLines[$j] -match '^\s*@' -and $j -gt ($i + 1))
                    ) {
                        break
                    }
                }

                if ($scenarioName) {
                    $scenarioRef = "$relative :: $scenarioName"
                    if ($tagScenarios -notcontains $scenarioRef) {
                        $tagScenarios += $scenarioRef
                    }
                }
            }
        }
    }

    $runtimeScenarios = @(
        Get-RuntimeEndpointCoverage `
            -Method $Method `
            -EndpointPath $EndpointPath
    )

    $runtimeCovered = ($runtimeScenarios.Count -gt 0)
    $tagCovered = ($tagScenarios.Count -gt 0)

    if ($runtimeCovered) {
        $effectiveScenarios = @($runtimeScenarios)
        $coverageSource = "runtime"
    }
    elseif ($tagCovered) {
        $effectiveScenarios = @($tagScenarios)
        $coverageSource = "tag"
    }
    else {
        $effectiveScenarios = @()
        $coverageSource = "none"
    }

    return [PSCustomObject]@{
        Tag              = $tag
        Covered          = ($runtimeCovered -or $tagCovered)
        CoverageSource   = $coverageSource
        Features         = @($features)
        Scenarios        = @($effectiveScenarios)
        TagScenarios     = @($tagScenarios)
        RuntimeScenarios = @($runtimeScenarios)
    }
}

function Get-UtoipaBlocksFromContent {
    param(
        [string]$Content,
        [string]$File
    )

    $results = @()
    if ([string]::IsNullOrWhiteSpace($Content)) { return @($results) }

    $lines = @($Content -split "`r?`n")
    $inside = $false
    $method = $null
    $endpointPath = $null
    $blockStart = -1
    $blockEnd = -1

    for ($i = 0; $i -lt $lines.Count; $i++) {
        $line = $lines[$i]

        if (-not $inside -and $line -match '^\s*#\[utoipa::path\(') {
            $inside = $true
            $method = $null
            $endpointPath = $null
            $blockStart = $i + 1
            continue
        }

        if (-not $inside) { continue }

        if (-not $method -and $line -match '^\s*(get|post|put|patch|delete),?\s*$') {
            $method = $Matches[1].ToUpperInvariant()
        }

        if (-not $endpointPath -and $line -match '^\s*path\s*=\s*"([^"]+)"') {
            $endpointPath = $Matches[1]
        }

        if ($line -match '^\s*\)\]\s*$') {
            $blockEnd = $i + 1

            if ($method -and $endpointPath) {
                $results += [PSCustomObject]@{
                    Method    = $method
                    Path      = $endpointPath
                    Module    = Get-EndpointModule -EndpointPath $endpointPath -File $File
                    File      = $File
                    StartLine = $blockStart
                    EndLine   = $blockEnd
                }
            }

            $inside = $false
            $method = $null
            $endpointPath = $null
            $blockStart = -1
            $blockEnd = -1
        }
    }

    return @($results)
}

function Get-GitFileContent {
    param(
        [string]$Repo,
        [string]$Ref,
        [string]$File
    )

    Push-Location $Repo
    try {
        $spec = "${Ref}:$File"
        $content = git show $spec 2>$null
        if ($LASTEXITCODE -ne 0) { return $null }
        return ($content -join [Environment]::NewLine)
    }
    finally {
        Pop-Location
    }
}

function Get-WorkingTreeFileContent {
    param(
        [string]$Repo,
        [string]$File
    )

    $full = Join-Path $Repo ($File.Replace("/", "\"))
    if (-not (Test-Path $full -PathType Leaf)) { return $null }
    return Get-Content $full -Raw -Encoding UTF8
}

function Get-ChangedLineNumbersFromDiff {
    param(
        [string[]]$DiffLines,
        [string]$TargetFile
    )

    $oldChanged = New-Object System.Collections.Generic.HashSet[int]
    $newChanged = New-Object System.Collections.Generic.HashSet[int]

    $currentFile = $null
    $oldLine = 0
    $newLine = 0
    $inHunk = $false

    foreach ($line in $DiffLines) {
        if ($line -match '^diff --git a/(.+) b/(.+)$') {
            $currentFile = Normalize-RepoPath $Matches[2]
            $inHunk = $false
            continue
        }

        if ($currentFile -ne (Normalize-RepoPath $TargetFile)) { continue }

        if ($line -match '^@@ -(\d+)(?:,\d+)? \+(\d+)(?:,\d+)? @@') {
            $oldLine = [int]$Matches[1]
            $newLine = [int]$Matches[2]
            $inHunk = $true
            continue
        }

        if (-not $inHunk) { continue }
        if ($line -match '^\\ No newline at end of file$') { continue }

        if ($line.StartsWith("+") -and -not $line.StartsWith("+++")) {
            [void]$newChanged.Add($newLine)
            $newLine++
            continue
        }

        if ($line.StartsWith("-") -and -not $line.StartsWith("---")) {
            [void]$oldChanged.Add($oldLine)
            $oldLine++
            continue
        }

        $oldLine++
        $newLine++
    }

    return [PSCustomObject]@{
        Old = $oldChanged
        New = $newChanged
    }
}

function Test-BlockTouched {
    param(
        [object]$Block,
        [System.Collections.Generic.HashSet[int]]$ChangedLines
    )

    if ($null -eq $Block -or $null -eq $ChangedLines) { return $false }

    foreach ($lineNumber in $ChangedLines) {
        if (
            $lineNumber -ge $Block.StartLine -and
            $lineNumber -le $Block.EndLine
        ) {
            return $true
        }
    }

    return $false
}

function Get-EndpointChangesFromRepositoryDiff {
    param(
        [string]$Repo,
        [object[]]$FileChanges,
        [string[]]$DiffLines,
        [string]$OldRef,
        [string]$NewRef,
        [switch]$WorkingTree
    )

    $results = @()

    $candidateFiles = @(
        $FileChanges |
        Where-Object {
            $_.Path -match '\.rs$' -and (
                $_.Path -match '^src/api/rest/openapi/' -or
                $_.Path -match '^src/api/rest/[^/]+\.rs$'
            )
        }
    )

    foreach ($change in $candidateFiles) {
        $file = Normalize-RepoPath $change.Path
        $oldFile = if ($change.OldPath) { Normalize-RepoPath $change.OldPath } else { $file }

        $oldContent = $null
        $newContent = $null

        if ($change.Status -ne "NEW") {
            $oldContent = Get-GitFileContent -Repo $Repo -Ref $OldRef -File $oldFile
        }

        if ($change.Status -ne "REMOVED") {
            if ($WorkingTree) {
                $newContent = Get-WorkingTreeFileContent -Repo $Repo -File $file
            }
            else {
                $newContent = Get-GitFileContent -Repo $Repo -Ref $NewRef -File $file
            }
        }

        $oldBlocks = @(Get-UtoipaBlocksFromContent -Content $oldContent -File $oldFile)
        $newBlocks = @(Get-UtoipaBlocksFromContent -Content $newContent -File $file)

        $lineChanges = Get-ChangedLineNumbersFromDiff -DiffLines $DiffLines -TargetFile $file

        # NEW endpoints: exist only in new content.
        foreach ($newBlock in $newBlocks) {
            $sameOld = @(
                $oldBlocks |
                Where-Object {
                    $_.Method -eq $newBlock.Method -and
                    $_.Path -eq $newBlock.Path
                }
            )

            if ($sameOld.Count -eq 0) {
                $results += [PSCustomObject]@{
                    Change = "NEW"
                    Method = $newBlock.Method
                    Path   = $newBlock.Path
                    Module = $newBlock.Module
                    File   = $newBlock.File
                }
                continue
            }

            # Same endpoint still exists. If any changed line is inside the
            # old or new utoipa block, its contract/documentation changed.
            $oldBlock = $sameOld[0]
            $oldTouched = Test-BlockTouched -Block $oldBlock -ChangedLines $lineChanges.Old
            $newTouched = Test-BlockTouched -Block $newBlock -ChangedLines $lineChanges.New

            if ($oldTouched -or $newTouched) {
                $results += [PSCustomObject]@{
                    Change = "CHANGED"
                    Method = $newBlock.Method
                    Path   = $newBlock.Path
                    Module = $newBlock.Module
                    File   = $newBlock.File
                }
            }
        }

        # REMOVED endpoints: exist only in old content.
        foreach ($oldBlock in $oldBlocks) {
            $sameNew = @(
                $newBlocks |
                Where-Object {
                    $_.Method -eq $oldBlock.Method -and
                    $_.Path -eq $oldBlock.Path
                }
            )

            if ($sameNew.Count -eq 0) {
                $results += [PSCustomObject]@{
                    Change = "REMOVED"
                    Method = $oldBlock.Method
                    Path   = $oldBlock.Path
                    Module = $oldBlock.Module
                    File   = $oldBlock.File
                }
            }
        }
    }

    return @(
        $results |
        Sort-Object Change, Method, Path, File -Unique
    )
}

function Convert-NameStatusToChanges {
    param([string[]]$NameStatusLines)

    $results = @()

    foreach ($line in $NameStatusLines) {
        if ([string]::IsNullOrWhiteSpace($line)) { continue }

        $parts = $line -split "`t"
        if ($parts.Count -lt 2) { continue }

        $rawStatus = $parts[0]
        $path = $null
        $oldPath = $null

        if ($rawStatus -match '^R' -and $parts.Count -ge 3) {
            $oldPath = Normalize-RepoPath $parts[1]
            $path = Normalize-RepoPath $parts[2]
            $status = "RENAMED"
        }
        else {
            $path = Normalize-RepoPath $parts[1]

            if ($rawStatus -match '^A') { $status = "NEW" }
            elseif ($rawStatus -match '^D') { $status = "REMOVED" }
            else { $status = "CHANGED" }
        }

        if (Is-IgnoredFile $path) { continue }

        $results += [PSCustomObject]@{
            Status  = $status
            Module  = Get-ModuleFromPath $path
            Path    = $path
            OldPath = $oldPath
            OpenApi = ($path -match "^src/api/rest/openapi/")
        }
    }

    return @($results)
}

function Convert-PorcelainToChanges {
    param([string[]]$StatusLines)

    $results = @()

    foreach ($line in $StatusLines) {
        if ([string]::IsNullOrWhiteSpace($line) -or $line.Length -lt 4) {
            continue
        }

        $rawStatus = $line.Substring(0, 2)
        $rawPath = $line.Substring(3).Trim()
        $oldPath = $null

        if ($rawPath -match '^(.+)\s+->\s+(.+)$') {
            $oldPath = Normalize-RepoPath $Matches[1]
            $path = Normalize-RepoPath $Matches[2]
        }
        else {
            $path = Normalize-RepoPath $rawPath
        }

        if (Is-IgnoredFile $path) { continue }

        if ($rawStatus -match '\?\?' -or $rawStatus -match 'A') {
            $status = "NEW"
        }
        elseif ($rawStatus -match 'D') {
            $status = "REMOVED"
        }
        elseif ($rawStatus -match 'R') {
            $status = "RENAMED"
        }
        else {
            $status = "CHANGED"
        }

        $results += [PSCustomObject]@{
            Status  = $status
            Module  = Get-ModuleFromPath $path
            Path    = $path
            OldPath = $oldPath
            OpenApi = ($path -match "^src/api/rest/openapi/")
        }
    }

    return @($results)
}

function Get-ModuleChanges {
    param(
        [object[]]$FileChanges,
        [string]$Repo,
        [string]$OldRef,
        [string]$NewRef,
        [switch]$WorkingTree
    )

    $interesting = @(
        $FileChanges |
        Where-Object {
            $_.Path -match '^src/api/rest/openapi/paths/[^/]+\.rs$' -or
            $_.Path -match '^src/api/rest/[^/]+\.rs$' -or
            $_.Path -match '^frontend/src/pages/[^/]+\.tsx$'
        }
    )

    if ($interesting.Count -eq 0) {
        return @()
    }

    function Get-ModulesAtRef {
        param(
            [string]$GitRef,
            [switch]$UseWorkingTree
        )

        $modules = New-Object System.Collections.Generic.HashSet[string]

        if ($UseWorkingTree) {
            $files = @(
                git -C $Repo ls-files --cached --others --exclude-standard
            )

            foreach ($file in $files) {
                $pathValue = Normalize-RepoPath $file

                if (
                    $pathValue -notmatch '^src/api/rest/openapi/paths/[^/]+\.rs$' -and
                    $pathValue -notmatch '^src/api/rest/[^/]+\.rs$' -and
                    $pathValue -notmatch '^frontend/src/pages/[^/]+\.tsx$'
                ) {
                    continue
                }

                $fullPath = Join-Path $Repo ($pathValue.Replace("/", "\"))

                if (-not (Test-Path $fullPath -PathType Leaf)) {
                    continue
                }

                $module = Get-ModuleFromPath $pathValue

                if (
                    -not [string]::IsNullOrWhiteSpace($module) -and
                    $module -ne "Other"
                ) {
                    [void]$modules.Add($module)
                }
            }
        }
        else {
            $files = @(
                git -C $Repo ls-tree -r --name-only $GitRef
            )

            foreach ($file in $files) {
                $pathValue = Normalize-RepoPath $file

                if (
                    $pathValue -notmatch '^src/api/rest/openapi/paths/[^/]+\.rs$' -and
                    $pathValue -notmatch '^src/api/rest/[^/]+\.rs$' -and
                    $pathValue -notmatch '^frontend/src/pages/[^/]+\.tsx$'
                ) {
                    continue
                }

                $module = Get-ModuleFromPath $pathValue

                if (
                    -not [string]::IsNullOrWhiteSpace($module) -and
                    $module -ne "Other"
                ) {
                    [void]$modules.Add($module)
                }
            }
        }

        return @($modules)
    }

    $oldModules = @(
        Get-ModulesAtRef -GitRef $OldRef
    )

    if ($WorkingTree) {
        $newModules = @(
            Get-ModulesAtRef -UseWorkingTree
        )
    }
    else {
        $newModules = @(
            Get-ModulesAtRef -GitRef $NewRef
        )
    }

    $results = @()

    foreach ($group in ($interesting | Group-Object Module)) {
        $moduleName = $group.Name

        if (
            [string]::IsNullOrWhiteSpace($moduleName) -or
            $moduleName -eq "Other"
        ) {
            continue
        }

        $existedBefore = $oldModules -contains $moduleName
        $existsAfter   = $newModules -contains $moduleName

        if (-not $existedBefore -and $existsAfter) {
            $moduleStatus = "NEW"
        }
        elseif ($existedBefore -and -not $existsAfter) {
            $moduleStatus = "REMOVED"
        }
        else {
            $moduleStatus = "CHANGED"
        }

        $files = @(
            $group.Group |
            ForEach-Object {
                if (
                    $_.Status -eq "REMOVED" -and
                    -not [string]::IsNullOrWhiteSpace($_.OldPath)
                ) {
                    $_.OldPath
                }
                else {
                    $_.Path
                }
            } |
            Sort-Object -Unique
        )

        $results += [PSCustomObject]@{
            Status = $moduleStatus
            Module = $moduleName
            Files  = $files
        }
    }

    return @($results | Sort-Object Module)
}

function Test-GitRef {
    param([string]$Ref)
    if ([string]::IsNullOrWhiteSpace($Ref)) { return $false }

    git rev-parse --verify --quiet "$Ref^{commit}" *> $null
    return ($LASTEXITCODE -eq 0)
}

if (-not (Test-Path $EcmRepo)) {
    throw "ECM repository bulunamadı: $EcmRepo"
}

if (-not (Test-Path $AutomationRepo)) {
    throw "Automation repository bulunamadı: $AutomationRepo"
}

Push-Location $EcmRepo

try {
    if (-not (Test-Path ".git")) {
        throw "ECM yolu bir Git repository değil: $EcmRepo"
    }

    $branchOutput = git branch --show-current
    $branch = if ($null -eq $branchOutput) { "" } else { "$branchOutput".Trim() }
    if ([string]::IsNullOrWhiteSpace($branch)) {
        $branch = "DETACHED_HEAD"
    }
    $remote = (git remote get-url origin).Trim()

    $comparisonMode = "WORKING_TREE"
    $comparisonLabel = "HEAD <-> working tree"

    if (-not [string]::IsNullOrWhiteSpace($BaseRef)) {
        if (-not (Test-GitRef $BaseRef)) {
            throw "BaseRef bulunamadı: $BaseRef"
        }

        if ([string]::IsNullOrWhiteSpace($HeadRef)) {
            $HeadRef = "HEAD"
        }

        if (-not (Test-GitRef $HeadRef)) {
            throw "HeadRef bulunamadı: $HeadRef"
        }

        $comparisonMode = "REF_RANGE"
        $comparisonLabel = "$BaseRef...$HeadRef"

        $nameStatusLines = @(git -c core.autocrlf=false diff --name-status --find-renames "$BaseRef...$HeadRef")
        $changes = @(Convert-NameStatusToChanges -NameStatusLines $nameStatusLines)

        $diffLines = @(
            git -c core.autocrlf=false diff --find-renames --unified=100000 "$BaseRef...$HeadRef" -- `
                "src/api/rest/openapi" `
                "src/api/rest/*.rs"
        )
    }
    else {
        $statusLines = @(git status --porcelain)
        $changes = @(Convert-PorcelainToChanges -StatusLines $statusLines)

        # Tracked staged + unstaged changes.
        $trackedDiff = @(
            git -c core.autocrlf=false diff HEAD --unified=100000 -- `
                "src/api/rest/openapi" `
                "src/api/rest/*.rs"
        )

        # git diff does not include untracked files. For a new untracked Rust
        # source file, synthesize an "added file" diff so new utoipa endpoints
        # are still detectable locally.
        $untrackedDiff = New-Object System.Collections.Generic.List[string]
        $untrackedFiles = @(
            git ls-files --others --exclude-standard -- `
                "src/api/rest/openapi" `
                "src/api/rest/*.rs"
        )

        foreach ($untracked in $untrackedFiles) {
            $u = Normalize-RepoPath $untracked
            if (Is-IgnoredFile $u) { continue }

            $full = Join-Path $EcmRepo ($u.Replace("/", "\"))
            if (-not (Test-Path $full -PathType Leaf)) { continue }

            $untrackedDiff.Add("--- /dev/null")
            $untrackedDiff.Add("+++ b/$u")

            foreach ($contentLine in @(Get-Content $full -Encoding UTF8)) {
                $untrackedDiff.Add("+$contentLine")
            }
        }

        $diffLines = @($trackedDiff) + @($untrackedDiff)
    }

    if ($comparisonMode -eq "REF_RANGE") {
        $endpointResults = @(
            Get-EndpointChangesFromRepositoryDiff `
                -Repo $EcmRepo `
                -FileChanges $changes `
                -DiffLines $diffLines `
                -OldRef $BaseRef `
                -NewRef $HeadRef
        )
    }
    else {
        $endpointResults = @(
            Get-EndpointChangesFromRepositoryDiff `
                -Repo $EcmRepo `
                -FileChanges $changes `
                -DiffLines $diffLines `
                -OldRef "HEAD" `
                -NewRef "HEAD" `
                -WorkingTree
        )
    }
}
finally {
    Pop-Location
}


if ($comparisonMode -eq "REF_RANGE") {
    $moduleResults = @(
        Get-ModuleChanges `
            -FileChanges $changes `
            -Repo $EcmRepo `
            -OldRef $BaseRef `
            -NewRef $HeadRef
    )
}
else {
    $moduleResults = @(
        Get-ModuleChanges `
            -FileChanges $changes `
            -Repo $EcmRepo `
            -OldRef "HEAD" `
            -NewRef "HEAD" `
            -WorkingTree
    )
}

$lines = New-Object System.Collections.Generic.List[string]
$endpointJson = @()

$lines.Add("ECM CHANGE / COVERAGE DETECTOR V3.1")
$lines.Add(("=" * 86))
$lines.Add("Repository : $EcmRepo")
$lines.Add("Branch     : $branch")
$lines.Add("Remote     : $remote")
$lines.Add("Mode       : $comparisonMode")
$lines.Add("Compare    : $comparisonLabel")
$lines.Add("Generated  : $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')")
$lines.Add("")

$lines.Add("MODULE CHANGES")
$lines.Add(("-" * 86))

if ($moduleResults.Count -eq 0) {
    $lines.Add("No module-level source changes detected.")
}
else {
    foreach ($module in $moduleResults) {
        $lines.Add(("{0,-10} {1}" -f $module.Status, $module.Module))
        foreach ($file in $module.Files) {
            $lines.Add("  - $file")
        }
    }
}

$lines.Add("")
$lines.Add("CHANGED FILES")
$lines.Add(("-" * 86))

if ($changes.Count -eq 0) {
    $lines.Add("No relevant changes detected.")
}
else {
    foreach ($change in $changes) {
        $lines.Add(
            ("{0,-10} {1,-24} {2}" -f $change.Status, $change.Module, $change.Path)
        )
    }
}

$lines.Add("")
$lines.Add("ENDPOINT CHANGES")
$lines.Add(("-" * 86))

$newCount = 0
$changedCount = 0
$removedCount = 0
$coveredCount = 0
$missingCoverage = 0
$affectedRemovedCount = 0

if ($endpointResults.Count -eq 0) {
    $lines.Add("No NEW, CHANGED or REMOVED utoipa endpoints detected.")
}
else {
    foreach ($endpoint in $endpointResults) {
        switch ($endpoint.Change) {
            "NEW"     { $newCount++ }
            "CHANGED" { $changedCount++ }
            "REMOVED" { $removedCount++ }
        }

        $coverage = Get-EndpointCoverage `
            -Method $endpoint.Method `
            -EndpointPath $endpoint.Path

        $coverageState = "MISSING"
        $action = ""

        if ($endpoint.Change -eq "REMOVED") {
            if ($coverage.Covered) {
                $coverageState = "AFFECTED"
                $affectedRemovedCount++
                $action = "REMOVE / UPDATE OBSOLETE AUTOMATION"
            }
            else {
                $coverageState = "NO EXPLICIT TEST"
                $action = "ENDPOINT REMOVAL REVIEW"
            }
        }
        elseif ($coverage.Covered) {
            $coverageState = "COVERED"
            $coveredCount++

            if ($endpoint.Change -eq "CHANGED") {
                $action = "EXISTING TEST FOUND - REVIEW ASSERTIONS"
            }
            elseif ($coverage.CoverageSource -eq "runtime") {
                $action = "RUNTIME COVERAGE FOUND"
            }
            else {
                $action = "EXISTING EXPLICIT COVERAGE FOUND"
            }
        }
        else {
            $missingCoverage++

            if ($endpoint.Change -eq "NEW") {
                $action = "NEW TEST REQUIRED"
            }
            else {
                $action = "COVERAGE REQUIRED FOR CHANGED ENDPOINT"
            }
        }

        $lines.Add("")
        $lines.Add(
            ("{0,-10} {1,-7} {2}" -f $endpoint.Change, $endpoint.Method, $endpoint.Path)
        )
        $lines.Add("Module   : $($endpoint.Module)")
        $lines.Add("Source   : $($endpoint.File)")
        $lines.Add("Tag      : $($coverage.Tag)")
        $lines.Add("Coverage : $coverageState")
        $lines.Add("Source   : $($coverage.CoverageSource.ToUpperInvariant())")
        $lines.Add("Action   : $action")

        if ($coverage.CoverageSource -eq "tag" -and $coverage.Features.Count -gt 0) {
            $lines.Add("Features :")
            foreach ($feature in $coverage.Features) {
                $lines.Add("  - $feature")
            }
        }

        if ($coverage.Scenarios.Count -gt 0) {
            $lines.Add("Scenarios:")
            foreach ($scenario in $coverage.Scenarios) {
                $lines.Add("  - $scenario")
            }
        }

        if (
            $coverage.CoverageSource -eq "runtime" -and
            $coverage.TagScenarios.Count -gt 0
        ) {
            $lines.Add("Tag scenarios (informational only):")
            foreach ($scenario in $coverage.TagScenarios) {
                $lines.Add("  - $scenario")
            }
        }

        $endpointJson += [PSCustomObject]@{
            Change        = $endpoint.Change
            Method        = $endpoint.Method
            Path          = $endpoint.Path
            Module        = $endpoint.Module
            Source        = $endpoint.File
            Tag              = $coverage.Tag
            Coverage         = $coverageState
            CoverageSource   = $coverage.CoverageSource
            Action           = $action
            Features         = @($coverage.Features)
            Scenarios        = @($coverage.Scenarios)
            TagScenarios     = @($coverage.TagScenarios)
            RuntimeScenarios = @($coverage.RuntimeScenarios)
        }
    }
}

$lines.Add("")
$lines.Add("NON-ENDPOINT CHANGES")
$lines.Add(("-" * 86))

$nonEndpointChanges = @(
    $changes |
    Where-Object {
        -not $_.OpenApi
    }
)

if ($nonEndpointChanges.Count -eq 0) {
    $lines.Add("No non-OpenAPI source changes detected.")
}
else {
    foreach ($change in $nonEndpointChanges) {
        $lines.Add(
            ("{0,-10} {1,-24} {2}" -f $change.Status, $change.Module, $change.Path)
        )
    }
}

# Exit policy:
# - Interactive/local default: report only, exit 0.
# - CI can pass -FailOnMissingCoverage to fail on uncovered NEW/CHANGED endpoints.
# - CI can additionally pass -FailOnRemovedCoveredEndpoint to fail when a removed
#   endpoint still has explicit automation that must be updated/removed.
$exitCode = 0
$ciResult = "PASS"

if (-not $NoFail) {
    if ($FailOnMissingCoverage -and $missingCoverage -gt 0) {
        $exitCode = 2
        $ciResult = "FAIL - MISSING ENDPOINT COVERAGE"
    }
    elseif ($FailOnRemovedCoveredEndpoint -and $affectedRemovedCount -gt 0) {
        $exitCode = 3
        $ciResult = "FAIL - REMOVED ENDPOINT HAS AFFECTED TESTS"
    }
}

if (
    $exitCode -eq 0 -and
    ($missingCoverage -gt 0 -or $affectedRemovedCount -gt 0)
) {
    $ciResult = "PASS (review required; fail policy not enabled)"
}

$lines.Add("")
$lines.Add("SUMMARY")
$lines.Add(("-" * 86))
$lines.Add("Relevant changed files : $($changes.Count)")
$lines.Add("Module changes         : $($moduleResults.Count)")
$lines.Add("New endpoints          : $newCount")
$lines.Add("Changed endpoints      : $changedCount")
$lines.Add("Removed endpoints      : $removedCount")
$lines.Add("Covered endpoints      : $coveredCount")
$lines.Add("Missing coverage       : $missingCoverage")
$lines.Add("Affected removed tests : $affectedRemovedCount")
$lines.Add("CI result              : $ciResult")
$lines.Add("Exit code              : $exitCode")

if ($missingCoverage -gt 0) {
    $lines.Add("Result                 : NEW / CHANGED TEST COVERAGE REQUIRED")
}
elseif ($affectedRemovedCount -gt 0) {
    $lines.Add("Result                 : REMOVED ENDPOINT AUTOMATION IMPACT FOUND")
}
elseif ($endpointResults.Count -gt 0) {
    $lines.Add("Result                 : ENDPOINT CHANGES COVERED / REVIEWED")
}
elseif ($changes.Count -gt 0) {
    $lines.Add("Result                 : SOURCE CHANGES REQUIRE REVIEW")
}
else {
    $lines.Add("Result                 : NO RELEVANT CHANGES")
}

$report = $lines -join [Environment]::NewLine

$report | Set-Content $reportFile -Encoding UTF8

$jsonReport = [PSCustomObject]@{
    Version = "3.1"
    Generated = (Get-Date).ToString("o")
    Repository = $EcmRepo
    Branch = $branch
    Remote = $remote
    Comparison = [PSCustomObject]@{
        Mode = $comparisonMode
        Label = $comparisonLabel
        BaseRef = $BaseRef
        HeadRef = $HeadRef
    }
    Modules = @($moduleResults)
    Files = @($changes)
    Endpoints = @($endpointJson)
    Summary = [PSCustomObject]@{
        ChangedFiles = $changes.Count
        ModuleChanges = $moduleResults.Count
        NewEndpoints = $newCount
        ChangedEndpoints = $changedCount
        RemovedEndpoints = $removedCount
        CoveredEndpoints = $coveredCount
        MissingCoverage = $missingCoverage
        AffectedRemovedTests = $affectedRemovedCount
        CiResult = $ciResult
        ExitCode = $exitCode
    }
}

$jsonReport |
    ConvertTo-Json -Depth 10 |
    Set-Content $jsonFile -Encoding UTF8

Write-Host ""
Write-Host $report
Write-Host ""
Write-Host "Text report:"
Write-Host $reportFile
Write-Host "JSON report:"
Write-Host $jsonFile

exit $exitCode


