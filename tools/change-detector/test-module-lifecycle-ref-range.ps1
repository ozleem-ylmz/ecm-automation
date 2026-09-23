
param(
    [string]$EcmRepo = "C:\Users\ozlemy\ECM"
)

$ErrorActionPreference = "Stop"

$detectorScript = Join-Path $PSScriptRoot "detect-changes.ps1"
$testRepo = Join-Path $env:TEMP "ecm-module-lifecycle-ref-range-test"

$probeRelativePath =
    "src/api/rest/openapi/paths/qa_ref_range_probe.rs"

$probeModuleName = "Qa Ref Range Probe"

function Write-TestStep {
    param([string]$Message)

    Write-Host ""
    Write-Host "======================================================================"
    Write-Host $Message
    Write-Host "======================================================================"
}

function Assert-Contains {
    param(
        [string[]]$Output,
        [string]$Expected,
        [string]$TestName
    )

    $text = $Output -join [Environment]::NewLine

    if ($text -notmatch [regex]::Escape($Expected)) {
        throw @"
TEST FAILED: $TestName

Expected output to contain:
$Expected

Actual output:
$text
"@
    }

    Write-Host "PASS: $TestName"
}

function Invoke-Detector {
    param(
        [string]$BaseRef,
        [string]$HeadRef
    )

    $output = @(
        & $detectorScript `
            -EcmRepo $testRepo `
            -BaseRef $BaseRef `
            -HeadRef $HeadRef `
            -NoFail *>&1 |
        ForEach-Object { $_.ToString() }
    )

    if ($LASTEXITCODE -ne 0) {
        throw "Detector process returned exit code $LASTEXITCODE."
    }

    return $output
}

function New-TestCommit {
    param(
        [string]$Message
    )

    git -C $testRepo add -A

    git -C $testRepo `
        -c user.name="QA Ref Range Test" `
        -c user.email="qa-ref-range@example.invalid" `
        commit -m $Message |
        Out-Null

    if ($LASTEXITCODE -ne 0) {
        throw "Could not create temporary commit: $Message"
    }

    $commit = (
        git -C $testRepo rev-parse HEAD
    ).Trim()

    if ($LASTEXITCODE -ne 0) {
        throw "Could not resolve temporary commit."
    }

    if ([string]::IsNullOrWhiteSpace($commit)) {
        throw "Temporary commit SHA is empty."
    }

    return $commit
}

if (-not (Test-Path $EcmRepo -PathType Container)) {
    throw "ECM repository not found: $EcmRepo"
}

$insideWorkTree =
    git -C $EcmRepo rev-parse --is-inside-work-tree 2>$null

if ($LASTEXITCODE -ne 0 -or $insideWorkTree -ne "true") {
    throw "Not a Git repository: $EcmRepo"
}

if (-not (Test-Path $detectorScript -PathType Leaf)) {
    throw "Detector script not found: $detectorScript"
}

try {
    Write-TestStep "Preparing isolated ECM worktree"

    if (Test-Path $testRepo) {
        git -C $EcmRepo worktree remove --force $testRepo 2>$null

        if (Test-Path $testRepo) {
            Remove-Item $testRepo -Recurse -Force
        }
    }

    git -C $EcmRepo worktree prune

    git -C $EcmRepo worktree add --detach $testRepo HEAD

    if ($LASTEXITCODE -ne 0) {
        throw "Could not create temporary worktree."
    }

    $probeFile =
        Join-Path `
            $testRepo `
            ($probeRelativePath.Replace("/", "\"))

    $probeDirectory =
        Split-Path $probeFile -Parent

    if (-not (Test-Path $probeDirectory)) {
        New-Item `
            -ItemType Directory `
            -Path $probeDirectory `
            -Force |
        Out-Null
    }

    #
    # BASELINE
    #

    $baselineCommit = (
        git -C $testRepo rev-parse HEAD
    ).Trim()

    if ($LASTEXITCODE -ne 0) {
        throw "Could not resolve baseline commit."
    }

    if ([string]::IsNullOrWhiteSpace($baselineCommit)) {
        throw "Baseline commit SHA is empty."
    }

    Write-Host ""
    Write-Host "Baseline commit: $baselineCommit"

    #
    # TEST 1 - NEW
    #

    Write-TestStep "TEST 1/3 - REF_RANGE NEW module"

    @"
// QA REF_RANGE lifecycle detector test module.
// Temporary file used only for automated verification.
pub fn qa_ref_range_probe() {}
"@ | Set-Content `
        -Path $probeFile `
        -Encoding UTF8

    $newCommit =
        New-TestCommit `
            -Message "test: add ref range probe module"

    Write-Host "Base: $baselineCommit"
    Write-Host "Head: $newCommit"

    $newOutput = @(
        Invoke-Detector `
            -BaseRef $baselineCommit `
            -HeadRef $newCommit
    )

    $newOutput |
        ForEach-Object { Write-Host $_ }

    Assert-Contains `
        -Output $newOutput `
        -Expected "NEW        $probeModuleName" `
        -TestName "REF_RANGE NEW module is detected"

    #
    # TEST 2 - CHANGED
    #

    Write-TestStep "TEST 2/3 - REF_RANGE CHANGED module"

    $existingContent =
        [System.IO.File]::ReadAllText($probeFile)

    $existingContent +=
        "`n// Changed REF_RANGE lifecycle verification.`n"

    $utf8NoBom =
        New-Object System.Text.UTF8Encoding($false)

    [System.IO.File]::WriteAllText(
        $probeFile,
        $existingContent,
        $utf8NoBom
    )

    $changedCommit =
        New-TestCommit `
            -Message "test: change ref range probe module"

    Write-Host "Base: $newCommit"
    Write-Host "Head: $changedCommit"

    $changedOutput = @(
        Invoke-Detector `
            -BaseRef $newCommit `
            -HeadRef $changedCommit
    )

    $changedOutput |
        ForEach-Object { Write-Host $_ }

    Assert-Contains `
        -Output $changedOutput `
        -Expected "CHANGED    $probeModuleName" `
        -TestName "REF_RANGE CHANGED module is detected"

    #
    # TEST 3 - REMOVED
    #

    Write-TestStep "TEST 3/3 - REF_RANGE REMOVED module"

    Remove-Item $probeFile

    $removedCommit =
        New-TestCommit `
            -Message "test: remove ref range probe module"

    Write-Host "Base: $changedCommit"
    Write-Host "Head: $removedCommit"

    $removedOutput = @(
        Invoke-Detector `
            -BaseRef $changedCommit `
            -HeadRef $removedCommit
    )

    $removedOutput |
        ForEach-Object { Write-Host $_ }

    Assert-Contains `
        -Output $removedOutput `
        -Expected "REMOVED    $probeModuleName" `
        -TestName "REF_RANGE REMOVED module is detected"

    Write-Host ""
    Write-Host "======================================================================"
    Write-Host "REF_RANGE MODULE LIFECYCLE TESTS PASSED"
    Write-Host "NEW      : PASS"
    Write-Host "CHANGED  : PASS"
    Write-Host "REMOVED  : PASS"
    Write-Host "======================================================================"
}
finally {
    Write-Host ""
    Write-Host "Cleaning temporary worktree..."

    if (Test-Path $testRepo) {
        git -C $testRepo reset --hard HEAD 2>$null |
            Out-Null
    }

    git -C $EcmRepo `
        worktree remove --force $testRepo 2>$null |
        Out-Null

    git -C $EcmRepo worktree prune 2>$null

    if (Test-Path $testRepo) {
        Remove-Item `
            $testRepo `
            -Recurse `
            -Force
    }

    Write-Host "Cleanup complete."
}

