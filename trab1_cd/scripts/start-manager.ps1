[CmdletBinding()]
param(
    [ValidateRange(1, 65535)]
    [int]$ManagerPort = 50051,

    [Parameter(Mandatory = $true)]
    [ValidateNotNullOrEmpty()]
    [string]$RedisHost,

    [ValidateRange(1, 65535)]
    [int]$RedisPort = 6379
)

$ErrorActionPreference = "Stop"
$repoRoot = Split-Path -Parent $PSScriptRoot
$projectDir = $repoRoot # ALTERADO: aponta diretamente para a raiz do projeto (trab1_cd)
$gradlew = Join-Path $projectDir "gradlew.bat"

if (-not (Test-Path $gradlew -PathType Leaf)) {
    throw "Não foi encontrado o Gradle Wrapper: $gradlew"
}

Push-Location $projectDir
try {
    $appArgs = "$ManagerPort $RedisHost $RedisPort"
    Write-Host "A iniciar Manager em 0.0.0.0:$ManagerPort"
    Write-Host "Redis configurado em ${RedisHost}:$RedisPort"
    # ALTERADO: Especifica a classe main do Manager
    & $gradlew ":app:run" "-PmainClass=grpcrunserverapp.manager.ManagerServer" "--args=$appArgs"
    if ($LASTEXITCODE -ne 0) {
        throw "O Manager terminou com código $LASTEXITCODE."
    }
}
finally {
    Pop-Location
}