# Scripts PowerShell do CodeRunner

Os scripts desta pasta são os pontos de entrada individuais para Windows/PowerShell.
Executam os projetos Gradle existentes; não substituem nem alteram a lógica Java.

## Pré-requisitos
- Windows com PowerShell 5.1 ou PowerShell 7.
- JDK 21 instalado e configurado no `PATH`.
- Docker Desktop/daemon iniciado para o Redis e para os RunServers que executam containers.
- Acesso à rede entre Manager, Redis, RunServers e Clients.

Executar os comandos a partir da raiz `CodeRunner` ou de qualquer outra diretoria.
Se a política de execução impedir scripts locais, pode ser necessário usar:
`Set-ExecutionPolicy -Scope CurrentUser RemoteSigned`

## Scripts individuais

### Redis
```powershell
.\scripts\start-redis.ps1
```
Parâmetros:
- `-ContainerName` (default `coderunner-redis`)
- `-HostPort` (default `6379`)
- `-HostAddress` (default `0.0.0.0`)
- `-RedisImage` (default `redis:7`)

O script reutiliza um container com o mesmo nome se já existir. Se estiver parado, inicia-o; se estiver ativo, não cria outro.

**Rede:** `0.0.0.0` publica a porta em todas as interfaces da máquina. Em máquinas expostas a redes não confiáveis, restringir `-HostAddress` e configurar firewall. Para várias VMs, usar no Manager o IP do Redis alcançável pelos RunServers, não `127.0.0.1`.

### Manager
```powershell
.\scripts\start-manager.ps1 -RedisHost 127.0.0.1
```
Parâmetros: `-ManagerPort` (50051), `-RedisHost` (obrigatório), `-RedisPort` (6379).
Corresponde a `:app:run --args="<portoManager> <redisIp> <redisPorto>"`.

### RunServer
```powershell
.\scripts\start-runserver.ps1 -ServerIP 127.0.0.1 -ManagerIP 127.0.0.1 -ServerPort 6001 -ManagerPort 50051
```
Parâmetros: `-ServerIP`, `-ServerPort`, `-ManagerIP`, `-ManagerPort`.
Corresponde a `:app:run --args="<serverIP> <serverPort> <managerIP> <managerPort>"`.
O `ServerIP` é o endereço anunciado ao Manager e tem de ser alcançável pelo Client e pelos outros RunServers.

### Client — submeter
```powershell
.\scripts\start-client.ps1 -Operation submit -Target .\programa.py
.\scripts\start-client.ps1 -Operation submit -Target .\programa.py -InputFile .\input.txt
```

### Client — consultar estado/resultado
```powershell
.\scripts\start-client.ps1 -Operation status -Target "<UUID>"
.\scripts\start-client.ps1 -Operation result -Target "<UUID>"
```
Parâmetros comuns do Client: `-ManagerHost` (127.0.0.1) e `-ManagerPort` (50051).
Para `submit`, `-Target` é o caminho do `.py` e `-InputFile` é opcional.
Para `status` e `result`, `-Target` é o UUID devolvido pela submissão.

## Ordem de arranque manual
1. Redis na máquina indicada ao Manager.
2. Manager, configurado com o IP/porto do Redis.
3. Um ou mais RunServers, cada um com porto próprio e endereço anunciável.
4. Client, para submeter ou consultar execuções.

Os scripts individuais não iniciam dependências automaticamente. Os scripts compostos serão criados numa fase seguinte e irão reutilizar estes pontos de entrada.
