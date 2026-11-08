package coderunner.runserver;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.PullImageResultCallback;
import com.github.dockerjava.api.command.WaitContainerResultCallback;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;

public class ContainerRunner {

    private static final String IMAGE = "python";
    private static final String TAG = "3.12-slim";

    private static final long MEMORY = 256L * 1024 * 1024;

    private static final String COMMAND =
            "timeout 10 python /work/prog.py " +
                    "< /work/input.txt > /work/out.txt " +
                    "2> /work/err.txt; echo $? > /work/exit.txt";

    @SuppressWarnings("UseSpecificCatch")
    public ExecutionResult execute(String requestId, Path dir)
            throws Exception {

        DockerClientConfig config =
                DefaultDockerClientConfig
                        .createDefaultConfigBuilder()
                        .build();

        ApacheDockerHttpClient http =
                new ApacheDockerHttpClient.Builder()
                        .dockerHost(config.getDockerHost())
                        .sslConfig(config.getSSLConfig())
                        .build();

        try (DockerClient docker =
                     DockerClientImpl.getInstance(config, http)) {

            pullIfMissing(docker);

            HostConfig hostConfig = HostConfig.newHostConfig()
                    .withNetworkMode("none")
                    .withMemory(MEMORY)
                    .withMemorySwap(MEMORY)
                    .withCpuPeriod(100000L)
                    .withCpuQuota(25000L)
                    .withBinds(new Bind(
                            dir.toString(),
                            new Volume("/work")
                    ));

            CreateContainerCmd create =
                    docker.createContainerCmd(IMAGE + ":" + TAG)
                            .withName("run-" + requestId)
                            .withHostConfig(hostConfig)
                            .withCmd("sh", "-c", COMMAND);

            String user = currentUser();

            if (user != null) {
                create.withUser(user);
            }

            String containerId = null;

            try {

                containerId = create.exec().getId();

                System.out.println(
                        "Container criado: " + containerId
                );

                docker.startContainerCmd(containerId).exec();

                docker.waitContainerCmd(containerId)
                        .exec(new WaitContainerResultCallback())
                        .awaitStatusCode();

            } finally {

                if (containerId != null) {

                    try {
                        docker.removeContainerCmd(containerId)
                                .withForce(true)
                                .exec();

                    } catch (Exception e) {
                        System.err.println(
                                "Erro ao remover container: "
                                        + e.getMessage()
                        );
                    }
                }
            }
        }

        int exitCode = readExitCode(
                dir.resolve("exit.txt")
        );

        String state;

        state = switch (exitCode) {
            case 0 -> "CONCLUIDO";
            case 124 -> "TIMEOUT";
            default -> "ERRO";
        };

        String stdout = readFile(dir.resolve("out.txt"));
        String stderr = readFile(dir.resolve("err.txt"));

        System.out.println(
                "Pedido " + requestId + ": " + state
        );

        return new ExecutionResult(
                state,
                stdout,
                stderr,
                exitCode
        );
    }

    private String currentUser() {

        if (System.getProperty("os.name")
                .startsWith("Windows")) {
            return null;
        }

        com.sun.security.auth.module.UnixSystem unix =
                new com.sun.security.auth.module.UnixSystem();

        return unix.getUid() + ":" + unix.getGid();
    }

    private int readExitCode(Path file)
            throws IOException {

        if (!Files.exists(file)) {
            return -1;
        }

        return Integer.parseInt(
                Files.readString(file).strip()
        );
    }

    private String readFile(Path file)
            throws IOException {

        if (!Files.exists(file)) {
            return "";
        }

        return Files.readString(file);
    }

    private void pullIfMissing(DockerClient docker)
            throws InterruptedException {

        try {

            docker.inspectImageCmd(IMAGE + ":" + TAG).exec();

        } catch (NotFoundException e) {

            System.out.println("A obter imagem Docker...");

            docker.pullImageCmd(IMAGE)
                    .withTag(TAG)
                    .exec(new PullImageResultCallback())
                    .awaitCompletion();
        }
    }
}
