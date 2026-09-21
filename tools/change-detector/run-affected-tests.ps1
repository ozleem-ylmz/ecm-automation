param(
    [string]$AutomationRepo = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path,
    [string]$ReportPath = "",
    [switch]$DryRun
)

$ErrorActionPreference = "Stop"

if ([string]::IsNullOrWhiteSpace($ReportPath)) {
    $ReportPath = Join-Path `
        $AutomationRepo `
        "target\change-detection\change-report-v3.1.json"
}

Write-Host ""
Write-Host "ECM AFFECTED TEST RUNNER"
Write-Host ("=" * 86)
Write-Host "Automation repo : $AutomationRepo"
Write-Host "Detector report : $ReportPath"

if (-not (Test-Path $ReportPath)) {
    Write-Error "Change detector JSON report was not found: $ReportPath"
    exit 10
}

$report = Get-Content $ReportPath -Raw | ConvertFrom-Json

$affectedEndpoints = @(
    $report.Endpoints |
        Where-Object {
            $_.Change -in @("NEW", "CHANGED") -and
            $_.Coverage -eq "COVERED" -and
            -not [string]::IsNullOrWhiteSpace($_.Tag)
        }
)

if ($affectedEndpoints.Count -eq 0) {
    Write-Host ""
    Write-Host "No covered NEW/CHANGED endpoints require test execution."
    Write-Host "Result : NO AFFECTED TESTS"
    exit 0
}

$tags = @(
    $affectedEndpoints |
        ForEach-Object { $_.Tag } |
        Sort-Object -Unique
)

Write-Host ""
Write-Host "AFFECTED ENDPOINTS"
Write-Host ("-" * 86)

foreach ($endpoint in $affectedEndpoints) {
    Write-Host ""
    Write-Host "$($endpoint.Change) $($endpoint.Method) $($endpoint.Path)"
    Write-Host "Module   : $($endpoint.Module)"
    Write-Host "Tag      : $($endpoint.Tag)"
    Write-Host "Coverage : $($endpoint.Coverage)"

    if ($endpoint.Scenarios.Count -gt 0) {
        Write-Host "Scenarios:"
        foreach ($scenario in $endpoint.Scenarios) {
            Write-Host "  - $scenario"
        }
    }
}

# Cucumber OR expression:
# @tag1 or @tag2 or @tag3
$tagExpression = $tags -join " or "

Write-Host ""
Write-Host "CUCUMBER FILTER"
Write-Host ("-" * 86)
Write-Host $tagExpression

if ($DryRun) {
    Write-Host ""
    Write-Host "DRY RUN - Maven was not executed."
    Write-Host "Result : AFFECTED TESTS FOUND"
    exit 0
}

Push-Location $AutomationRepo

try {
    Write-Host ""
    Write-Host "RUNNING AFFECTED CUCUMBER TESTS"
    Write-Host ("-" * 86)

    & mvn test "-Dcucumber.filter.tags=$tagExpression"

    $mavenExitCode = $LASTEXITCODE

    Write-Host ""
    Write-Host ("-" * 86)

    if ($mavenExitCode -ne 0) {
        Write-Host "Result    : FAILED"
        Write-Host "Exit code : $mavenExitCode"
        exit $mavenExitCode
    }

    Write-Host "Result    : PASSED"
    Write-Host "Exit code : 0"
    exit 0
}
finally {
    Pop-Location
}
