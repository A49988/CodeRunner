package manager.server;

import redis.clients.jedis.Jedis;

import java.util.Map;
import java.util.Optional;

/**
 * Acesso partilhado ao Redis do CodeRunner.
 *
 * Esquema acordado com o RunServer:
 *   chave: execution:<UUID>
 *   campos: estado, servidor, resultadoObtido
 *
 * O RunServer deve criar/atualizar estes campos. O Client consulta
 * "servidor" para encaminhar pedidos para o RunServer proprietário.
 */
public class RedisRepository implements AutoCloseable {

    public static final String KEY_PREFIX = "execution:";

    private final String host;
    private final int port;

    public RedisRepository(String host, int port) {
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("O host do Redis é obrigatório.");
        }
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("Porto Redis inválido: " + port);
        }
        this.host = host;
        this.port = port;
    }

    private Jedis connect() {
        return new Jedis(host, port);
    }

    /** Cria os metadados iniciais de uma execução. Chamado pelo RunServer. */
    public void registerExecution(String executionId, String serverIp, int serverPort) {
        String key = key(executionId);
        try (Jedis jedis = connect()) {
            jedis.hset(key, Map.of(
                    "estado", "PENDENTE",
                    "servidor", serverIp + ":" + serverPort,
                    "resultadoObtido", "false"
            ));
        }
    }

    /** Atualiza o estado da execução. Chamado pelo RunServer. */
    public void updateStatus(String executionId, String status) {
        try (Jedis jedis = connect()) {
            jedis.hset(key(executionId), "estado", status);
        }
    }

    /** Devolve os metadados existentes para uma execução. */
    public Optional<ExecutionInfo> getExecution(String executionId) {
        try (Jedis jedis = connect()) {
            Map<String, String> fields = jedis.hgetAll(key(executionId));
            if (fields == null || fields.isEmpty()) {
                return Optional.empty();
            }

            String server = fields.get("servidor");
            String ip = fields.get("ownerIp");
            String portText = fields.get("ownerPort");

            // Compatibilidade com o esquema ownerIp/ownerPort usado em versões anteriores.
            if ((server == null || server.isBlank()) && ip != null && portText != null) {
                server = ip + ":" + portText;
            }
            if (server == null || server.isBlank()) {
                throw new IllegalStateException(
                        "A execução " + executionId + " não tem servidor proprietário no Redis."
                );
            }

            int separator = server.lastIndexOf(':');
            if (separator <= 0 || separator == server.length() - 1) {
                throw new IllegalStateException("Endereço do RunServer inválido no Redis: " + server);
            }

            String serverIp = server.substring(0, separator);
            int serverPort = Integer.parseInt(server.substring(separator + 1));
            return Optional.of(new ExecutionInfo(
                    executionId,
                    fields.getOrDefault("estado", "DESCONHECIDO"),
                    serverIp,
                    serverPort
            ));
        }
    }

    public Optional<ExecutionInfo> getOwner(String executionId) {
        return getExecution(executionId);
    }

    private String key(String executionId) {
        if (executionId == null || executionId.isBlank()) {
            throw new IllegalArgumentException("UUID da execução obrigatório.");
        }
        return KEY_PREFIX + executionId;
    }

    @Override
    public void close() {
        // Cada operação abre e fecha a sua ligação Jedis.
    }

    public record ExecutionInfo(
            String executionId,
            String status,
            String serverIp,
            int serverPort
    ) {}
}
