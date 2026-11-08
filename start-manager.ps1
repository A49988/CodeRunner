param(
    [int]$ManagerPort = 50051,
    [string]$RedisIp = "127.0.0.1",
    [int]$RedisPort = 6000
)

$ProjectDir = Join-Path $PSScriptRoot "CodeRunner"
$GradleWrapper = Join-Path $ProjectDir "gradlew.bat"

if (-not (Test-Path $GradleWrapper)) {
    Write-Host "Error: Gradle Wrapper not found at:" -ForegroundColor Red
    Write-Host $GradleWrapper
    Write-Host "Make sure this script is located in the parent directory of CodeRunner."
    exit 1
}

Write-Host "Starting ManagerServer..." -ForegroundColor Cyan
Write-Host "Project:      $ProjectDir"
Write-Host "Port:         $ManagerPort"
Write-Host "Redis:        ${RedisIp}:$RedisPort"
Write-Host ""
Write-Host "To stop the server, press Ctrl+C." -ForegroundColor Yellow
Write-Host ""

Push-Location $ProjectDir
try {
    & $GradleWrapper :app:run "--args=$ManagerPort $RedisIp $RedisPort"
    exit $LASTEXITCODE
}
finally {
    Pop-Location
}