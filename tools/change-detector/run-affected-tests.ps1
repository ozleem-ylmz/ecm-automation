param(
    [string]$AutomationRepo = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path,
    [string]$ReportFile = "",
    [switch]$DryRun
)

$ErrorActionPreference = "Stop"

if ([string]::IsNullOrWhiteSpace($ReportFile)) {
    $ReportFile = Join-Path `
        $AutomationRepo `
        "target\change-detection\change-report-v3.1.json"
}

if (-not (Test-Path $AutomationRepo -PathType Container)) {
    throw "Automation repository bulunamadı: $AutomationRepo"
}

if (-not (Test-Path $ReportFile -PathType Leaf)) {
    throw "Change detection JSON report bulunamadı: $ReportFile"
}

Write-Host ""
Write-Host "ECM AFFECTED TEST RUNNER"
Write-Host ("=" * 86)
Write-Host "Repository : $AutomationRepo"
Write-Host "Report     : $ReportFile"

try {
    $report = Get-Content $ReportFile -Raw -Encoding UTF8 |
        ConvertFrom-Json
}
catch {
    throw "Change detection JSON report okunamadı: $($_.Exception.Message)"
}

$affectedTests = @(
    $report.AffectedTests |
        Where-Object {
            -not [string]::IsNullOrWhiteSpace("$_")
        } |
        Sort-Object -Unique
)

Write-Host "Affected   : $($affectedTests.Count)"
Write-Host ""

if ($affectedTests.Count -eq 0) {
    Write-Host "No affected tests found."
    Write-Host "Maven will not be started."
    exit 0
}

$scenarioNames = @()

foreach ($affectedTest in $affectedTests) {
    $value = "$affectedTest"

    if ($value -match '^\s*(.+?)\s+::\s+(.+?)\s*$') {
        $scenarioName = $Matches[2].Trim()
    }
    else {
        $scenarioName = $value.Trim()
    }

    if (-not [string]::IsNullOrWhiteSpace($scenarioName)) {
        $scenarioNames += $scenarioName
    }
}

$scenarioNames = @(
    $scenarioNames |
        Sort-Object -Unique
)

if ($scenarioNames.Count -eq 0) {
    throw "AffectedTests bulundu ancak scenario adı çıkarılamadı."
}

Write-Host "SCENARIOS"
Write-Host ("-" * 86)

foreach ($scenarioName in $scenarioNames) {
    Write-Host "  - $scenarioName"
}

$escapedScenarioNames = @(
    $scenarioNames |
        ForEach-Object {
            [regex]::Escape($_)
        }
)

$filterParts = @(
    $escapedScenarioNames |
        ForEach-Object {
            "^$_`$"
        }
)

$cucumberFilter = $filterParts -join "|"

Write-Host ""
Write-Host "CUCUMBER FILTER"
Write-Host ("-" * 86)
Write-Host $cucumberFilter
Write-Host ""

if ($DryRun) {
    Write-Host "DRY RUN - Maven was not started."
    exit 0
}

Push-Location $AutomationRepo

try {
    Write-Host "RUNNING AFFECTED TESTS"
    Write-Host ("-" * 86)

    & mvn test "-Dcucumber.filter.name=$cucumberFilter"

    $mavenExitCode = $LASTEXITCODE
}
finally {
    Pop-Location
}

Write-Host ""

if ($mavenExitCode -eq 0) {
    Write-Host "AFFECTED TEST RUN SUCCESS"
}
else {
    Write-Host "AFFECTED TEST RUN FAILED - Maven exit code: $mavenExitCode"
}

exit $mavenExitCode
