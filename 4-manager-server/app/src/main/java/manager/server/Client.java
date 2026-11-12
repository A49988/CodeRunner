package manager.server;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import servicestubs.managerclient.ProtoVoid;
import servicestubs.managerclient.ServiceGrpc;
import servicestubs.managerclient.ServerId;
import servicestubs.runserver.GetResultRequest;
import servicestubs.runserver.GetResultResponse;
import servicestubs.runserver.GetStatusRequest;
import servicestubs.runserver.GetStatusResponse;
import servicestubs.runserver.RunServerServiceGrpc;
import servicestubs.runserver.SubmitProgramRequest;
import servicestubs.runserver.SubmitProgramResponse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Cliente de linha de comandos do CodeRunner.
 *
 * submit <managerHost> <managerPort> <program.py> [input.txt]
 * status <managerHost> <managerPort> <executionId>
 * result <managerHost> <managerPort> <executionId>
 *
 * O Client escolhe um RunServer através do Manager. Para status/result pode
 * contactar qualquer RunServer: esse RunServer deve consultar o Redis e
 * encaminhar o pedido para o servidor proprietário do UUID.
 */
public final class Client {
    private static final int CHUNK_SIZE = 64 * 1024;
    private static final long SUBMIT_TIMEOUT_SECONDS = 30;
    private static final long RESULT_TIMEOUT_SECONDS = 90;

    private Client() {}

    public static void main(String[] args) {
        try {
            if (args.length == 0) {
                usage();
                return;
            }
            switch (args[0].toLowerCase()) {
                case "submit" -> {
                    if (args.length < 4 || args.length > 5) {
                        usage();
                        return;
                    }
                    submit(args[1], Integer.parseInt(args[2]), Path.of(args[3]),
                            args.length == 5 ? Path.of(args[4]) : null);
                }
                case "status" -> {
                    requireArgs(args, 4);
                    showStatus(args[1], Integer.parseInt(args[2]), args[3]);
                }
                case "result" -> {
                    requireArgs(args, 4);
                    showResult(args[1], Integer.parseInt(args[2]), args[3]);
                }
                default -> usage();
            }
        } catch (Exception e) {
            System.err.println("Erro no Client: " + e.getMessage());
        }
    }

    /** Pede um RunServer ao Manager e envia-lhe o programa e o stdin por streaming. */
    public static String submit(String managerHost, int managerPort,
                                Path programFile, Path inputFile) throws Exception {
        if (programFile == null || !Files.isRegularFile(programFile)) {
            throw new IOException("Ficheiro do programa nao encontrado: " + programFile);
        }
        if (!programFile.getFileName().toString().toLowerCase().endsWith(".py")) {
            throw new IOException("O ficheiro submetido tem de ter extensao .py.");
        }

        byte[] program = Files.readAllBytes(programFile);
        if (inputFile != null && !Files.isRegularFile(inputFile)) {
            throw new IOException("Ficheiro de input nao encontrado: " + inputFile);
        }
        byte[] stdin = inputFile == null ? new byte[0] : Files.readAllBytes(inputFile);

        ServerId selected = selectRunServer(managerHost, managerPort);

        System.out.printf("RunServer selecionado: %s:%d%n", selected.getIp(), selected.getPort());

        ManagedChannel runServerChannel = channel(selected.getIp(), selected.getPort());
        try {
            String executionId = submitToRunServer(runServerChannel, program, stdin);
            System.out.println("Programa submetido com sucesso.");
            System.out.println("UUID: " + executionId);
            return executionId;
        } finally {
            shutdown(runServerChannel);
        }
    }

    private static String submitToRunServer(ManagedChannel channel, byte[] program,
                                             byte[] stdin) throws Exception {
        CountDownLatch finished = new CountDownLatch(1);
        AtomicReference<String> executionId = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();

        StreamObserver<SubmitProgramResponse> responseObserver = new StreamObserver<>() {
            @Override public void onNext(SubmitProgramResponse response) {
                executionId.set(response.getExecutionId());
            }
            @Override public void onError(Throwable error) {
                failure.set(error);
                finished.countDown();
            }
            @Override public void onCompleted() { finished.countDown(); }
        };

        StreamObserver<SubmitProgramRequest> requestObserver =
                RunServerServiceGrpc.newStub(channel)
                        .withDeadlineAfter(SUBMIT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                        .submitProgram(responseObserver);
        try {
            if (program.length == 0) {
                requestObserver.onNext(SubmitProgramRequest.newBuilder()
                        .setStdin(ByteString.copyFrom(stdin)).setIsLast(true).build());
            } else {
                for (int offset = 0; offset < program.length; offset += CHUNK_SIZE) {
                    int end = Math.min(offset + CHUNK_SIZE, program.length);
                    SubmitProgramRequest.Builder request = SubmitProgramRequest.newBuilder()
                            .setData(ByteString.copyFrom(Arrays.copyOfRange(program, offset, end)))
                            .setIsLast(end == program.length);
                    // O stdin é enviado apenas no primeiro bloco.
                    if (offset == 0) request.setStdin(ByteString.copyFrom(stdin));
                    requestObserver.onNext(request.build());
                }
            }
            requestObserver.onCompleted();
        } catch (RuntimeException e) {
            requestObserver.onError(e);
            throw e;
        }

        if (!finished.await(SUBMIT_TIMEOUT_SECONDS + 2, TimeUnit.SECONDS)) {
            throw new IOException("Tempo esgotado ao submeter o programa.");
        }
        if (failure.get() != null) {
            throw new IOException("O RunServer recusou a submissao.", failure.get());
        }
        if (executionId.get() == null || executionId.get().isBlank()) {
            throw new IOException("O RunServer nao devolveu o UUID da execuçao.");
        }
        return executionId.get();
    }

    /** Consulta o estado através de um RunServer escolhido pelo Manager. */
    public static String showStatus(String managerHost, int managerPort, String executionId)
            throws Exception {
        validateExecutionId(executionId);
        ServerId selected = selectRunServer(managerHost, managerPort);
        System.out.println("RunServer escolhido para consulta: "
        + selected.getIp() + ":" + selected.getPort());
        ManagedChannel runServerChannel = channel(selected.getIp(), selected.getPort());
        try {
            GetStatusResponse response = RunServerServiceGrpc.newBlockingStub(runServerChannel)
                    .withDeadlineAfter(15, TimeUnit.SECONDS)
                    .getStatus(GetStatusRequest.newBuilder().setExecutionId(executionId).build());
            System.out.println("Estado da execucao " + executionId + ": " + response.getStatus());
            return response.getStatus();
        } catch (StatusRuntimeException e) {
            throw new IOException("Nao foi possivel consultar o estado no RunServer: " + e.getStatus(), e);
        } finally {
            shutdown(runServerChannel);
        }
    }

    /**
     * Consulta o resultado por server-streaming. O RunServer contactado deve
     * consultar o Redis para localizar o proprietário do UUID e encaminhar o pedido.
     * stdout/stderr são acumulados a partir dos blocos recebidos.
     */
    public static GetResultResponse showResult(
            String managerHost, int managerPort, String executionId) throws Exception {
        validateExecutionId(executionId);
        ServerId selected = selectRunServer(managerHost, managerPort);
        System.out.println("RunServer escolhido para consulta: "
        + selected.getIp() + ":" + selected.getPort());
        ManagedChannel runServerChannel = channel(selected.getIp(), selected.getPort());
        try {
            GetResultRequest request = GetResultRequest.newBuilder()
                    .setExecutionId(executionId).build();
            Iterator<GetResultResponse> stream = RunServerServiceGrpc.newBlockingStub(runServerChannel)
                    .withDeadlineAfter(RESULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .getResult(request);

            ByteArrayOutputStream stdout = new ByteArrayOutputStream();
            ByteArrayOutputStream stderr = new ByteArrayOutputStream();
            String status = "DESCONHECIDO";
            int exitCode = -1;
            boolean received = false;
            boolean finished = false;

            while (stream.hasNext()) {
                GetResultResponse chunk = stream.next();
                received = true;
                if (!chunk.getStatus().isBlank()) status = chunk.getStatus();
                if (!chunk.getStdout().isEmpty()) chunk.getStdout().writeTo(stdout);
                if (!chunk.getStderr().isEmpty()) chunk.getStderr().writeTo(stderr);
                exitCode = chunk.getExitCode();
                finished = chunk.getFinished();
            }
            if (!received) throw new IOException("O RunServer terminou o stream sem devolver resultado.");
            if (!finished) throw new IOException("O stream terminou sem marcar o resultado como completo.");

            GetResultResponse result = GetResultResponse.newBuilder()
                    .setStatus(status)
                    .setStdout(ByteString.copyFrom(stdout.toByteArray()))
                    .setStderr(ByteString.copyFrom(stderr.toByteArray()))
                    .setExitCode(exitCode)
                    .setFinished(true)
                    .build();

            System.out.println("Estado: " + status);
            System.out.println("Saida (stdout):");
            System.out.print(result.getStdout().toStringUtf8());
            if (result.getStdout().isEmpty()) System.out.println("(vazio)");
            System.out.println("\nErros (stderr):");
            System.out.print(result.getStderr().toStringUtf8());
            if (result.getStderr().isEmpty()) System.out.println("(vazio)");
            System.out.println("\nCodigo de saida: " + exitCode);
            return result;
        } catch (StatusRuntimeException e) {
            throw new IOException("Nao foi possivel obter o resultado: " + e.getStatus(), e);
        } finally {
            shutdown(runServerChannel);
        }
    }

    private static ServerId selectRunServer(String managerHost, int managerPort) throws IOException {
        ManagedChannel managerChannel = channel(managerHost, managerPort);
        try {
            ServerId selected = ServiceGrpc.newBlockingStub(managerChannel)
                    .withDeadlineAfter(10, TimeUnit.SECONDS)
                    .getServer(ProtoVoid.getDefaultInstance());
            if (selected.getIp().isBlank() || selected.getPort() < 1) {
                throw new IOException("O Manager devolveu um endereço de RunServer invalido.");
            }
            return selected;
        } catch (StatusRuntimeException e) {
            throw new IOException("Nao foi possivel obter um RunServer no Manager: " + e.getStatus(), e);
        } finally {
            shutdown(managerChannel);
        }
    }

    private static ManagedChannel channel(String host, int port) {
        if (host == null || host.isBlank() || port < 1 || port > 65535) {
            throw new IllegalArgumentException("Endereço gRPC inválido: " + host + ":" + port);
        }
        return ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
    }

    private static void validateExecutionId(String executionId) {
        if (executionId == null || executionId.isBlank()) {
            throw new IllegalArgumentException("UUID da execuçao obrigatório.");
        }
    }

    private static void shutdown(ManagedChannel channel) {
        channel.shutdown();
        try {
            if (!channel.awaitTermination(2, TimeUnit.SECONDS)) channel.shutdownNow();
        } catch (InterruptedException e) {
            channel.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private static void requireArgs(String[] args, int count) {
        if (args.length != count) {
            usage();
            throw new IllegalArgumentException("Número de argumentos invalido.");
        }
    }

    private static void usage() {
        System.out.println("""
                CodeRunner Client

                Submeter programa:
                  runClient --args="submit <managerHost> <managerPort> <program.py> [input.txt]"

                Consultar estado:
                  runClient --args="status <managerHost> <managerPort> <executionId>"

                Consultar resultado:
                  runClient --args="result <managerHost> <managerPort> <executionId>"
                """);
    }
}
