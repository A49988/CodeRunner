package coderunner.runserver;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * Repositório dos metadados globais das execuções do CodeRunner.
 *
 * O Redis guarda apenas metadados (estado e RunServer proprietário).
 * stdout/stderr permanecem no RunServer que executou o pedido, como exige o enunciado.
 *
 * Chave: execution:<UUID>
 * Campos: estado, servidor, ownerIp, ownerPort, resultadoObtido, criadoEm
 */
public class RedisRepository implements AutoCloseable {
    public static final String KEY_PREFIX = "execution:";
    private static final Set<String> VALID_STATUSES = Set.of(
            "PENDENTE", "EXECUTANDO", "CONCLUIDO", "TIMEOUT", "ERRO"
    );

    private final JedisPool pool;

    public RedisRepository(String host, int port) {
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("O host do Redis é obrigatório.");
        }
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("Porto Redis inválido: " + port);
        }

        JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxTotal(16);
        config.setMaxIdle(8);
        config.setMinIdle(1);
        config.setTestOnBorrow(true);
        config.setMaxWait(Duration.ofSeconds(3));
        this.pool = new JedisPool(config, host, port);
    }

    /** Regista uma nova execução antes de o RunServer iniciar o processamento. */
    public void registerExecution(String executionId, String serverIp, int serverPort) {
        validateExecutionId(executionId);
        if (serverIp == null || serverIp.isBlank()) {
            throw new IllegalArgumentException("IP do RunServer obrigatório.");
        }
        if (serverPort < 1 || serverPort > 65535) {
            throw new IllegalArgumentException("Porto do RunServer inválido.");
        }

        String key = key(executionId);
        try (Jedis jedis = pool.getResource()) {
            if (jedis.exists(key)) {
                throw new IllegalStateException("Já existe uma execução com o UUID " + executionId);
            }
            jedis.hset(key, Map.of(
                    "estado", "PENDENTE",
                    // Mantém o formato usado pela primeira versão do repositório.
                    "servidor", serverIp + ":" + serverPort,
                    // Campos separados evitam ambiguidades ao interpretar IPv6.
                    "ownerIp", serverIp,
                    "ownerPort", Integer.toString(serverPort),
                    "resultadoObtido", "false",
                    "criadoEm", Long.toString(System.currentTimeMillis())
            ));
        }
    }

    /** Atualiza o estado de uma execução. Chamado pelo RunServer. */
    public void updateStatus(String executionId, String status) {
        validateStatus(status);
        try (Jedis jedis = pool.getResource()) {
            String key = key(executionId);
            if (!jedis.exists(key)) {
                throw new IllegalArgumentException("Execução não encontrada: " + executionId);
            }
            jedis.hset(key, "estado", status.toUpperCase());
        }
    }

    /** Obtém estado e endereço do RunServer que possui os ficheiros de resultado. */
    public Optional<ExecutionInfo> getExecution(String executionId) {
        try (Jedis jedis = pool.getResource()) {
            Map<String, String> fields = jedis.hgetAll(key(executionId));
            if (fields == null || fields.isEmpty()) return Optional.empty();

            String ip = fields.get("ownerIp");
            String portText = fields.get("ownerPort");

            // Compatibilidade com dados guardados pela versão anterior.
            if ((ip == null || portText == null) && fields.get("servidor") != null) {
                String address = fields.get("servidor");
                int separator = address.lastIndexOf(':');
                if (separator <= 0 || separator == address.length() - 1) {
                    throw new IllegalStateException("Endereço do RunServer inválido no Redis: " + address);
                }
                ip = address.substring(0, separator);
                portText = address.substring(separator + 1);
            }
            if (ip == null || ip.isBlank() || portText == null) {
                throw new IllegalStateException("A execução não tem RunServer proprietário registado.");
            }

            int ownerPort;
            try {
                ownerPort = Integer.parseInt(portText);
            } catch (NumberFormatException e) {
                throw new IllegalStateException("Porto do RunServer inválido no Redis: " + portText, e);
            }
            return Optional.of(new ExecutionInfo(
                    executionId,
                    fields.getOrDefault("estado", "DESCONHECIDO"),
                    ip,
                    ownerPort,
                    Boolean.parseBoolean(fields.getOrDefault("resultadoObtido", "false")),
                    fields.get("criadoEm")
            ));
        }
    }

    /** Atalho para obter o proprietário de uma execução. */
    public Optional<ExecutionInfo> getOwner(String executionId) {
        return getExecution(executionId);
    }

    /** Marca que o Client já recebeu o resultado; não apaga os ficheiros por si só. */
    public void markResultRetrieved(String executionId) {
        try (Jedis jedis = pool.getResource()) {
            String key = key(executionId);
            if (!jedis.exists(key)) {
                throw new IllegalArgumentException("Execução não encontrada: " + executionId);
            }
            jedis.hset(key, "resultadoObtido", "true");
        }
    }

    /** Remove os metadados depois de o RunServer ter eliminado os ficheiros locais. */
    public void deleteExecution(String executionId) {
        try (Jedis jedis = pool.getResource()) {
            jedis.del(key(executionId));
        }
    }

    private static void validateStatus(String status) {
        if (status == null || !VALID_STATUSES.contains(status.toUpperCase())) {
            throw new IllegalArgumentException("Estado inválido: " + status + ". Valores: " + VALID_STATUSES);
        }
    }

    private static void validateExecutionId(String executionId) {
        if (executionId == null || executionId.isBlank()) {
            throw new IllegalArgumentException("UUID da execução obrigatório.");
        }
    }

    private static String key(String executionId) {
        validateExecutionId(executionId);
        return KEY_PREFIX + executionId;
    }

    @Override
    public void close() {
        pool.close();
    }

    public record ExecutionInfo(
            String executionId,
            String status,
            String serverIp,
            int serverPort,
            boolean resultRetrieved,
            String createdAt
    ) {}
}
