
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
                    "O programa Python nao pode estar vazio"
            );
        }

        String id = UUID.randomUUID().toString();

        Path dir = requestsDir.resolve(id);

        Files.createDirectories(dir);

        Files.write(dir.resolve("prog.py"), data.getProgram());
        Files.write(dir.resolve("input.txt"), data.getStdin());

        boolean registered = false;
        try {
            // O registo PENDENTE tem de existir antes de o trabalho ser agendado.
            redis.createRequest(id, serverIp, serverPort);
            registered = true;
            executor.submit(() -> execute(id, dir));
            return id;
        } catch (RuntimeException e) {
            if (registered) {
                try {
                    redis.updateState(id, "ERRO");
                } catch (Exception ignored) {
                    e.addSuppressed(ignored);
                }
            }
            deleteDirectory(dir);
            throw e;
        }
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
        try {
            if (!executor.awaitTermination(30, java.util.concurrent.TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
