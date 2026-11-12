[CmdletBinding()]
param(
    [string]$ContainerName = "coderunner-redis",
    [int]$HostPort = 6379,
    [string]$HostAddress = "0.0.0.0",
    [string]$RedisImage = "redis:7"
)

$ErrorActionPreference = "Stop"

if ($HostPort -lt 1 -or $HostPort -gt 65535) {
    throw "HostPort tem de estar entre 1 e 65535."
}

# Confirma que o Docker está instalado e o daemon está acessível.
docker info *> $null
if ($LASTEXITCODE -ne 0) {
    throw "Não foi possível contactar o Docker. Confirma que o Docker Desktop/daemon está iniciado."
}

$existing = docker ps -a --filter "name=^/$ContainerName$" --format "{{.Names}}"
if ($LASTEXITCODE -ne 0) {
    throw "Falha ao consultar os containers Docker."
}

if ($existing -contains $ContainerName) {
    $running = docker inspect --format "{{.State.Running}}" $ContainerName
    if ($LASTEXITCODE -ne 0) { throw "Não foi possível inspecionar o container '$ContainerName'." }

    if ($running -eq "true") {
        Write-Host "Redis já está em execução: $ContainerName"
    } else {
        Write-Host "A iniciar o container Redis existente: $ContainerName"
        docker start $ContainerName
        if ($LASTEXITCODE -ne 0) { throw "Não foi possível iniciar o container Redis." }
    }
} else {
    Write-Host "A criar e iniciar o Redis ($RedisImage) em ${HostAddress}:$HostPort..."
    docker run -d --name $ContainerName -p "${HostAddress}:${HostPort}:6379" $RedisImage
    if ($LASTEXITCODE -ne 0) { throw "Não foi possível criar o container Redis." }
}

Write-Host "Redis disponível na porta $HostPort (container: $ContainerName)."