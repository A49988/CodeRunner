
package grpcrunserverapp;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.util.Map;

public class RedisManager implements AutoCloseable {

    private final JedisPool pool;

    public RedisManager(String ip, int port) {

        JedisPoolConfig config = new JedisPoolConfig();

        config.setMaxTotal(20);
        config.setMaxIdle(10);
        config.setMinIdle(2);

        this.pool = new JedisPool(config, ip, port);
    }

    private String key(String id) {
        return "request:" + id;
    }

    // Registar um novo pedido
    public void createRequest(
            String id,
            String serverIp,
            int serverPort) {

        try (Jedis jedis = pool.getResource()) {

            String key = key(id);

            jedis.hset(key, Map.of(
                    "id", id,
                    "state", "PENDENTE",
                    "serverIp", serverIp,
                    "serverPort", String.valueOf(serverPort)
            ));

        }
    }

    // Atualizar o estado
    public void updateState(String id, String state) {

        try (Jedis jedis = pool.getResource()) {

            jedis.hset(key(id), "state", state);

        }
    }

    // Obter todos os dados de um pedido
    public Map<String, String> getRequest(String id) {

        try (Jedis jedis = pool.getResource()) {

            return jedis.hgetAll(key(id));

        }
    }

    // Verificar se o pedido existe
    public boolean exists(String id) {

        try (Jedis jedis = pool.getResource()) {

            return jedis.exists(key(id));

        }
    }

    @Override
    public void close() {
        pool.close();
    }
}
