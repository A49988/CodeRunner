
package grpcrunserverapp;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RequestManager implements AutoCloseable {

    private final RedisManager redis;
    private final ContainerRunner runner;

    private final String serverIp;
    private final int serverPort;

    private final Path requestsDir;

    private final ExecutorService executor =
            Executors.newFixedThreadPool(5);

    public RequestManager(
            RedisManager redis,
            ContainerRunner runner,
            String serverIp,
            int serverPort) throws IOException {

        this.redis = redis;
        this.runner = runner;
        this.serverIp = serverIp;
        this.serverPort = serverPort;

        requestsDir = Path.of("requests")
                .toAbsolutePath()
                .normalize();

        Files.createDirectories(requestsDir);
    }

    public String submit(RequestData data) throws IOException {

        if (data.getProgram().length == 0) {
            throw new IllegalArgumentException(
                    "O programa Python não pode estar vazio"
            );
        }

        String id = UUID.randomUUID().toString();

        Path dir = requestsDir.resolve(id);

        Files.createDirectories(dir);

        Files.write(dir.resolve("prog.py"), data.getProgram());
        Files.write(dir.resolve("input.txt"), data.getStdin());

        redis.createRequest(id, serverIp, serverPort);

        try {
            executor.submit(() -> execute(id, dir));
        } catch (RuntimeException e) {
            redis.updateState(id, "ERRO");
            deleteDirectory(dir);
            throw e;
        }

        return id;
    }

    private void execute(String id, Path dir) {

        try {

            redis.updateState(id, "EM_EXECUCAO");

            ExecutionResult result =
                    runner.execute(id, dir);

            redis.updateState(id, result.getState());

        } catch (Exception e) {

            e.printStackTrace();

            try {
                redis.updateState(id, "ERRO");
            } catch (Exception redisError) {
                redisError.printStackTrace();
            }
        }
    }

    public Path getRequestDirectory(String id) {
        return requestsDir.resolve(id);
    }

    public void deleteDirectory(Path dir) {

        if (!Files.exists(dir)) {
            return;
        }

        try (var paths = Files.walk(dir)) {

            paths.sorted((a, b) -> b.compareTo(a))
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void close() {
        executor.shutdown();
    }
}
