$ErrorActionPreference = "Stop"

$repositoryRoot = Split-Path -Parent $PSScriptRoot
$backendDirectory = Join-Path $repositoryRoot "backend"
$reportsDirectory = Join-Path $repositoryRoot "reports"
$jacocoReport = Join-Path $reportsDirectory "jacoco\index.html"
$jacocoXmlReport = Join-Path $reportsDirectory "jacoco\jacoco.xml"

Push-Location $backendDirectory
try {
    Write-Host "JaCoCo: running unit tests and generating coverage report..."
    & mvn -B clean verify
    if ($LASTEXITCODE -ne 0) {
        throw "Maven verify failed."
    }
} finally {
    Pop-Location
}

Write-Host "JaCoCo HTML report: $jacocoReport"
Write-Host "JaCoCo XML report:   $jacocoXmlReport"
