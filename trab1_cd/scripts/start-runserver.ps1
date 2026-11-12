[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [ValidateNotNullOrEmpty()]
    [string]$ServerIP,

    [ValidateRange(1, 65535)]
    [int]$ServerPort = 6001,

    [Parameter(Mandatory = $true)]
    [ValidateNotNullOrEmpty()]
    [string]$ManagerIP,

    [ValidateRange(1, 65535)]
    [int]$ManagerPort = 50051
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
    $appArgs = "$ServerIP $ServerPort $ManagerIP $ManagerPort"
    Write-Host "A iniciar RunServer em ${ServerIP}:$ServerPort"
    Write-Host "Manager configurado em ${ManagerIP}:$ManagerPort"
    # ALTERADO: Especifica a classe main do RunServer
    & $gradlew ":app:run" "-PmainClass=grpcrunserverapp.server.RunServer" "--args=$appArgs"
    if ($LASTEXITCODE -ne 0) {
        throw "O RunServer terminou com código $LASTEXITCODE."
    }
}
finally {
    Pop-Location
}