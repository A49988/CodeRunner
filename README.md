# CodeRunner — TPA1 Computação Distribuída

## Estrutura
- `4-manager-server`: ManagerServer e Client de linha de comandos.
- `5-runserver`: RunServer, execução Docker, Redis e serviço gRPC.
- `4-manager-server/contracts/src/main/proto/runserver_contract.proto` e
  `5-runserver/contracts/src/main/proto/run_server_contract.proto` definem o
  mesmo contrato gRPC entre Client e RunServer. Não alterar um sem atualizar o outro.
- `1-redis-kv-example` e `3-docker-java-examples` são exemplos auxiliares.

## Pré-requisitos
- JDK 21
- Docker em execução em cada máquina que aloja um RunServer
- Redis acessível pelo Manager e por todos os RunServers
- Imagem `python:3.12-slim` previamente obtida em cada VM:
  `docker pull python:3.12-slim`
- Gradle Wrapper incluído nos projetos.

## 1. Iniciar Redis
Na VM do Manager, por exemplo:
```bash
docker run -d --name coderunner-redis -p 6379:6379 redis:7
```
Se Redis já estiver instalado/gerido, usar o respetivo IP e porto.
**Importante:** em ambiente com várias VMs, não anunciar `127.0.0.1` como IP
do Redis. O Manager deve devolver o IP privado/público da VM do Redis que seja
alcançável a partir de todos os RunServers.

## 2. Iniciar ManagerServer
A partir de `4-manager-server`:
```bash
./gradlew :app:run --args="50051 127.0.0.1 6379"
```
No Windows:
```bat
gradlew.bat :app:run --args="50051 127.0.0.1 6379"
```
Argumentos: `<portoManager> <redisIp> <redisPorto>`.

## 3. Iniciar cada RunServer
A partir de `5-runserver`, em cada VM:
```bash
./gradlew :app:run --args="127.0.0.1 6001 <IP_DO_MANAGER> 50051"
```
Argumentos: `<serverIP> <serverPort> <managerIP> <managerPort>`.
Usar um porto diferente por RunServer. O IP anunciado tem de ser alcançável pelos Clients e pelos outros RunServers.
O endereço Redis é recebido do Manager durante o registo.

## 4. Usar o Client
A partir de `4-manager-server`:
```bash
./gradlew :app:runClient --args="submit <IP_DO_MANAGER> 50051 programa.py [input.txt]"
./gradlew :app:runClient --args="status <IP_DO_MANAGER> 50051 <UUID>"
./gradlew :app:runClient --args="result <IP_DO_MANAGER> 50051 <UUID>"
```
O comando `submit` devolve o UUID. Guardar esse identificador para consultar o estado e o resultado.
O Client pede ao Manager um RunServer para cada operação. Para consultar um UUID,
o RunServer contactado consulta o Redis e encaminha o pedido para o RunServer
que executou o programa.

## Estados
- `PENDENTE`: pedido registado no Redis, à espera de execução.
- `EM_EXECUCAO`: container em execução.
- `CONCLUIDO`: processo terminou com código 0.
- `TIMEOUT`: limite de 10 segundos excedido (código 124).
- `ERRO`: outro código de saída ou falha durante a execução.

## Segurança e ficheiros
Cada pedido usa um container `python:3.12-slim`, sem rede, com limite de 256 MB
de memória e 10 segundos. `stdout`, `stderr` e `exit.txt` são guardados na
diretoria local do RunServer. O Redis guarda apenas estado e endereço do
RunServer proprietário, não os outputs. Depois de um resultado final ser
entregue, os ficheiros locais desse pedido são apagados.

## Demonstração
Para cumprir a demonstração descrita no enunciado, iniciar pelo menos três
RunServers distribuídos por pelo menos duas VMs e executar pelo menos um Client
ligado a cada RunServer.
