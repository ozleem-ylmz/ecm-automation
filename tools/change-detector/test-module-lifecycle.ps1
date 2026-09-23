param(
    [string]$EcmRepo = "C:\Users\ozlemy\ECM"
)

$ErrorActionPreference = "Stop"

$automationRepo = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
$detectorScript = Join-Path $PSScriptRoot "detect-changes.ps1"
$testRepo = Join-Path $env:TEMP "ecm-module-lifecycle-test"
$probeRelativePath = "src/api/rest/openapi/paths/qa_lifecycle_probe.rs"
$probeModuleName = "Qa Lifecycle Probe"

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
    $output = @(
        & $detectorScript `
            -EcmRepo $testRepo `
            -NoFail *>&1 |
        ForEach-Object { $_.ToString() }
    )

    if ($LASTEXITCODE -ne 0) {
        throw "Detector process returned exit code $LASTEXITCODE."
    }

    return $output
}

if (-not (Test-Path $EcmRepo -PathType Container)) {
    throw "ECM repository not found: $EcmRepo"
}

if (-not (Test-Path (Join-Path $EcmRepo ".git"))) {
    $insideWorkTree = git -C $EcmRepo rev-parse --is-inside-work-tree 2>$null

    if ($LASTEXITCODE -ne 0 -or $insideWorkTree -ne "true") {
        throw "Not a Git repository: $EcmRepo"
    }
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

# Keep line endings stable inside the disposable test worktree.
# This prevents Git's LF/CRLF warning from being promoted to a
# NativeCommandError by PowerShell while detect-changes.ps1 runs.


$probeFile = Join-Path $testRepo ($probeRelativePath.Replace("/", "\"))

    Write-TestStep "TEST 1/3 - NEW module"

    @"
// QA lifecycle detector test module.
// Temporary file used only for automated change-detection verification.
pub fn qa_lifecycle_probe() {}
"@ | Set-Content -Path $probeFile -Encoding UTF8

    $newOutput = @(Invoke-Detector)

    $newOutput | ForEach-Object { Write-Host $_ }

    Assert-Contains `
        -Output $newOutput `
        -Expected "NEW        $probeModuleName" `
        -TestName "NEW module is detected"

    git -C $testRepo add $probeRelativePath

    git -C $testRepo `
        -c user.name="QA Lifecycle Test" `
        -c user.email="qa-lifecycle@example.invalid" `
        commit -m "test: add lifecycle probe module"

    if ($LASTEXITCODE -ne 0) {
        throw "Could not commit temporary lifecycle module."
    }

    Write-TestStep "TEST 2/3 - CHANGED module"

    $existingContent = [System.IO.File]::ReadAllText($probeFile)
$existingContent += "`n// Changed module lifecycle verification.`n"

$utf8NoBom = New-Object System.Text.UTF8Encoding($false)

[System.IO.File]::WriteAllText(
    $probeFile,
    $existingContent,
    $utf8NoBom
)

    $changedOutput = @(Invoke-Detector)

    $changedOutput | ForEach-Object { Write-Host $_ }

    Assert-Contains `
        -Output $changedOutput `
        -Expected "CHANGED    $probeModuleName" `
        -TestName "CHANGED module is detected"

    git -C $testRepo restore $probeRelativePath

    Write-TestStep "TEST 3/3 - REMOVED module"

    Remove-Item $probeFile

    $removedOutput = @(Invoke-Detector)

    $removedOutput | ForEach-Object { Write-Host $_ }

    Assert-Contains `
        -Output $removedOutput `
        -Expected "REMOVED    $probeModuleName" `
        -TestName "REMOVED module is detected"

    Write-Host ""
    Write-Host "======================================================================"
    Write-Host "MODULE LIFECYCLE TESTS PASSED"
    Write-Host "NEW      : PASS"
    Write-Host "CHANGED  : PASS"
    Write-Host "REMOVED  : PASS"
    Write-Host "======================================================================"
}
finally {
    Write-Host ""
    Write-Host "Cleaning temporary worktree..."

    if (Test-Path $testRepo) {
        git -C $testRepo reset --hard HEAD 2>$null | Out-Null
    }

    git -C $EcmRepo worktree remove --force $testRepo 2>$null | Out-Null
    git -C $EcmRepo worktree prune 2>$null

    if (Test-Path $testRepo) {
        Remove-Item $testRepo -Recurse -Force
    }

    Write-Host "Cleanup complete."
}