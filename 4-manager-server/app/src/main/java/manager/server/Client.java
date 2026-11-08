package manager.server;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import servicestubs.managerclient.ProtoVoid;
import servicestubs.managerclient.ServiceGrpc;
import servicestubs.runserver.GetResultRequest;
import servicestubs.runserver.GetResultResponse;
import servicestubs.runserver.GetStatusRequest;
import servicestubs.runserver.GetStatusResponse;
import servicestubs.runserver.RunServerServiceGrpc;
import servicestubs.runserver.SubmitProgramRequest;
import servicestubs.runserver.SubmitProgramResponse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Cliente de linha de comandos do CodeRunner.
 *
 * Comandos:
 *   submit <managerHost> <managerPort> <program.py> [input.txt]
 *   status <managerHost> <managerPort> <executionId>
 *   result <managerHost> <managerPort> <executionId>
 */
public final class Client {
    private static final int CHUNK_SIZE = 64 * 1024;

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
            e.printStackTrace(System.err);
        }
    }

    /** Pede um RunServer ao Manager e envia-lhe o programa e o input. */
    public static String submit(String managerHost, int managerPort,
                                Path programFile, Path inputFile) throws Exception {
        if (!Files.isRegularFile(programFile)) {
            throw new IOException("Ficheiro do programa não encontrado: " + programFile);
        }
        byte[] program = Files.readAllBytes(programFile);
        byte[] stdin = inputFile == null ? new byte[0] : Files.readAllBytes(inputFile);

        ManagedChannel managerChannel = channel(managerHost, managerPort);
        try {
            var manager = ServiceGrpc.newBlockingStub(managerChannel);
            var selected = manager.getServer(ProtoVoid.getDefaultInstance());
            System.out.println("RunServer selecionado: " + selected.getIp() + ":" + selected.getPort());

            ManagedChannel runServerChannel = channel(selected.getIp(), selected.getPort());
            try {
                String executionId = submitToRunServer(runServerChannel, program, stdin);
                System.out.println("Programa submetido com sucesso.");
                System.out.println("UUID: " + executionId);
                return executionId;
            } finally {
                runServerChannel.shutdownNow();
            }
        } finally {
            managerChannel.shutdownNow();
        }
    }

    private static String submitToRunServer(ManagedChannel channel, byte[] program,
                                             byte[] stdin) throws Exception {
        var stub = RunServerServiceGrpc.newStub(channel);
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

        StreamObserver<SubmitProgramRequest> requestObserver = stub.submitProgram(responseObserver);
        try {
            if (program.length == 0) {
                requestObserver.onNext(SubmitProgramRequest.newBuilder()
                        .setStdin(ByteString.copyFrom(stdin)).setIsLast(true).build());
            } else {
                for (int offset = 0; offset < program.length; offset += CHUNK_SIZE) {
                    int end = Math.min(offset + CHUNK_SIZE, program.length);
                    byte[] chunk = Arrays.copyOfRange(program, offset, end);
                    SubmitProgramRequest.Builder request = SubmitProgramRequest.newBuilder()
                            .setData(ByteString.copyFrom(chunk))
                            .setIsLast(end == program.length);
                    // O stdin só é enviado no primeiro bloco para não ser duplicado.
                    if (offset == 0) request.setStdin(ByteString.copyFrom(stdin));
                    requestObserver.onNext(request.build());
                }
            }
            requestObserver.onCompleted();
        } catch (Exception e) {
            requestObserver.onError(e);
            throw e;
        }

        if (!finished.await(30, TimeUnit.SECONDS)) {
            throw new IOException("Tempo esgotado ao submeter o programa.");
        }
        if (failure.get() != null) {
            throw new IOException("O RunServer recusou a submissão.", failure.get());
        }
        if (executionId.get() == null || executionId.get().isBlank()) {
            throw new IOException("O RunServer não devolveu o UUID da execução.");
        }
        return executionId.get();
    }

    /**
     * Pede ao Manager qualquer RunServer e consulta o estado através dele.
     * O RunServer contactado deve usar o Redis para localizar/encaminhar
     * a consulta caso o pedido pertença a outro RunServer.
     */
    public static String showStatus(String managerHost, int managerPort, String executionId)
            throws Exception {
        var selected = selectRunServer(managerHost, managerPort);
        ManagedChannel runServerChannel = channel(selected.getIp(), selected.getPort());
        try {
            GetStatusResponse response = RunServerServiceGrpc.newBlockingStub(runServerChannel)
                    .getStatus(GetStatusRequest.newBuilder().setExecutionId(executionId).build());
            System.out.println("Estado da execução " + executionId + ": " + response.getStatus());
            return response.getStatus();
        } finally {
            runServerChannel.shutdownNow();
        }
    }

    /**
     * Pede ao Manager qualquer RunServer e solicita-lhe o resultado.
     * Se o pedido pertencer a outro servidor, o RunServer contactado
     * deve descobrir o proprietário no Redis e encaminhar a operação.
     */
    public static GetResultResponse showResult(
            String managerHost, int managerPort, String executionId) throws Exception {
        var selected = selectRunServer(managerHost, managerPort);
        ManagedChannel runServerChannel = channel(selected.getIp(), selected.getPort());
        try {
            GetResultResponse result = RunServerServiceGrpc.newBlockingStub(runServerChannel)
                    .getResult(GetResultRequest.newBuilder().setExecutionId(executionId).build());
            System.out.println("Saída (stdout):");
            System.out.println(result.getStdout().toStringUtf8());
            System.out.println("Erros (stderr):");
            System.out.println(result.getStderr().toStringUtf8());
            System.out.println("Código de saída: " + result.getExitCode());
            return result;
        } finally {
            runServerChannel.shutdownNow();
        }
    }

    private static servicestubs.managerclient.ServerId selectRunServer(
            String managerHost, int managerPort) {
        ManagedChannel managerChannel = channel(managerHost, managerPort);
        try {
            return ServiceGrpc.newBlockingStub(managerChannel)
                    .getServer(ProtoVoid.getDefaultInstance());
        } finally {
            managerChannel.shutdownNow();
        }
    }

    private static ManagedChannel channel(String host, int port) {
        return ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
    }

    private static void requireArgs(String[] args, int count) {
        if (args.length != count) {
            usage();
            throw new IllegalArgumentException("Número de argumentos inválido.");
        }
    }

    private static void usage() {
        System.out.println("""
                CodeRunner Client

                Submeter:
                  runClient --args="submit <managerHost> <managerPort> <program.py> [input.txt]"

                Consultar estado:
                  runClient --args="status <managerHost> <managerPort> <executionId>"

                Consultar resultado:
                  runClient --args="result <managerHost> <managerPort> <executionId>"
                """);
    }
}
