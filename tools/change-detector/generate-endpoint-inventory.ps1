param(
    [string]$OpenApiUrl = "http://localhost:8080/api-docs/openapi.json",

    [string]$OutputPath = (
        Join-Path $PSScriptRoot "..\..\src\test\resources\coverage\endpoint-inventory.csv"
    )
)

$ErrorActionPreference = "Stop"

Write-Host ""
Write-Host "ECM ENDPOINT INVENTORY GENERATOR"
Write-Host "--------------------------------"
Write-Host "OpenAPI : $OpenApiUrl"
Write-Host "Output  : $OutputPath"
Write-Host ""

try {
    Write-Host "Loading OpenAPI specification..."

    $spec = Invoke-RestMethod `
        -Uri $OpenApiUrl `
        -Method Get

} catch {
    Write-Error "OpenAPI specification could not be loaded from $OpenApiUrl"
    Write-Error $_
    exit 1
}

if ($null -eq $spec.paths) {
    Write-Error "OpenAPI specification does not contain a paths section."
    exit 1
}

$httpMethods = @(
    "get",
    "post",
    "put",
    "patch",
    "delete"
)

$endpoints = foreach ($pathProperty in $spec.paths.PSObject.Properties) {

    $path = $pathProperty.Name
    $pathDefinition = $pathProperty.Value

    foreach ($operationProperty in $pathDefinition.PSObject.Properties) {

        $method = $operationProperty.Name.ToLower()

        if ($method -notin $httpMethods) {
            continue
        }

        $operation = $operationProperty.Value

        $module = ""

        if (
            $null -ne $operation.tags -and
            $operation.tags.Count -gt 0
        ) {
            $module = [string]$operation.tags[0]
        }

        if ([string]::IsNullOrWhiteSpace($module)) {
            $module = "other"
        }

        [PSCustomObject]@{
            Module = $module
            Method = $method.ToUpper()
            Path   = $path
        }
    }
}

$endpoints = @(
    $endpoints |
        Sort-Object Method, Path -Unique
)

if ($endpoints.Count -eq 0) {
    Write-Error "No API endpoints were found in the OpenAPI specification."
    exit 1
}

$outputDirectory = Split-Path -Parent $OutputPath

if (-not (Test-Path $outputDirectory)) {
    New-Item `
        -ItemType Directory `
        -Path $outputDirectory `
        -Force |
        Out-Null
}

$endpoints |
    Export-Csv `
        -Path $OutputPath `
        -NoTypeInformation `
        -Encoding UTF8

Write-Host ""
Write-Host "Endpoint inventory generated successfully."
Write-Host ""
Write-Host "Total endpoints : $($endpoints.Count)"
Write-Host ""

Write-Host "By module:"
$endpoints |
    Group-Object Module |
    Sort-Object Name |
    ForEach-Object {
        Write-Host ("  {0,-24} {1,4}" -f $_.Name, $_.Count)
    }

Write-Host ""
Write-Host "By method:"
$endpoints |
    Group-Object Method |
    Sort-Object Name |
    ForEach-Object {
        Write-Host ("  {0,-8} {1,4}" -f $_.Name, $_.Count)
    }

Write-Host ""
Write-Host "Inventory:"
Write-Host (Resolve-Path $OutputPath)
Write-Host ""