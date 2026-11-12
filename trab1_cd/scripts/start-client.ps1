[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [ValidateSet("submit", "status", "result")]
    [string]$Operation,

    [string]$ManagerHost = "127.0.0.1",

    [ValidateRange(1, 65535)]
    [int]$ManagerPort = 50051,

    [Parameter(Mandatory = $true)]
    [ValidateNotNullOrEmpty()]
    [string]$Target,

    [string]$InputFile
)

$ErrorActionPreference = "Stop"
$repoRoot = Split-Path -Parent $PSScriptRoot
$projectDir = $repoRoot # ALTERADO: aponta diretamente para a raiz do projeto (trab1_cd)
$gradlew = Join-Path $projectDir "gradlew.bat"

if (-not (Test-Path $gradlew -PathType Leaf)) {
    throw "Não foi encontrado o Gradle Wrapper: $gradlew"
}

function ConvertTo-GradleArgument([string]$Value) {
    # O Gradle interpreta --args com regras de argumentos separadas por espaços.
    # Aspas permitem preservar caminhos que contenham espaços.
    return '"' + ($Value -replace '\\', '\\' -replace '"', '\"') + '"'
}

switch ($Operation) {
    "submit" {
        if (-not (Test-Path $Target -PathType Leaf)) {
            throw "Ficheiro Python não encontrado: $Target"
        }
        $programPath = (Resolve-Path $Target).Path
        if ([IO.Path]::GetExtension($programPath) -ne ".py") {
            throw "O ficheiro submetido tem de ter extensão .py."
        }

        $arguments = @("submit", $ManagerHost, "$ManagerPort", $programPath)
        if ($InputFile) {
            if (-not (Test-Path $InputFile -PathType Leaf)) {
                throw "Ficheiro de input não encontrado: $InputFile"
            }
            $arguments += (Resolve-Path $InputFile).Path
        }
    }
    "status" { $arguments = @("status", $ManagerHost, "$ManagerPort", $Target) }
    "result" { $arguments = @("result", $ManagerHost, "$ManagerPort", $Target) }
}

$gradleArgs = ($arguments | ForEach-Object { ConvertTo-GradleArgument $_ }) -join " "

Push-Location $projectDir
try {
    Write-Host "A executar Client: $Operation (Manager ${ManagerHost}:$ManagerPort)"
    # ALTERADO: Especifica a classe main do cliente para funcionar no módulo unificado
    & $gradlew ":app:run" "-PmainClass=grpcrunserverapp.client.Client" "--args=$gradleArgs"
    if ($LASTEXITCODE -ne 0) {
        throw "O Client terminou com código $LASTEXITCODE."
    }
}
finally {
    Pop-Location
}