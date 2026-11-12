package grpcrunserverapp;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import servicestubs.runserver.*;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class ServiceClient extends RunServerServiceGrpc.RunServerServiceImplBase {
    private static final int CHUNK_SIZE = 64 * 1024;

    private final RequestManager requestManager;
    private final RedisManager redis;
    private final String serverIp;
    private final int serverPort;

    public ServiceClient(RequestManager requestManager, RedisManager redis,
                         String serverIp, int serverPort) {
        this.requestManager = requestManager;
        this.redis = redis;
        this.serverIp = serverIp;
        this.serverPort = serverPort;
    }

    @Override
    public StreamObserver<SubmitProgramRequest> submitProgram(
            StreamObserver<SubmitProgramResponse> responseObserver) {
        RequestData data = new RequestData();
        return new StreamObserver<>() {
            private boolean failed;

            @Override public void onNext(SubmitProgramRequest chunk) {
                if (failed) return;
                try {
                    if (!chunk.getData().isEmpty()) {
                        data.addProgram(chunk.getData().toByteArray());
                    }
                    if (!chunk.getStdin().isEmpty()) {
                        data.addStdin(chunk.getStdin().toByteArray());
                    }
                } catch (Exception e) {
                    failed = true;
                    responseObserver.onError(Status.INVALID_ARGUMENT
                            .withDescription("Bloco de submissao inválido")
                            .withCause(e).asRuntimeException());
                }
            }

            @Override public void onError(Throwable error) {
                failed = true;
            }

            @Override public void onCompleted() {
                if (failed) return;
                try {
                    String id = requestManager.submit(data);
                    responseObserver.onNext(SubmitProgramResponse.newBuilder()
                            .setExecutionId(id).build());
                    responseObserver.onCompleted();
                } catch (IllegalArgumentException e) {
                    responseObserver.onError(Status.INVALID_ARGUMENT
                            .withDescription(e.getMessage()).asRuntimeException());
                } catch (Exception e) {
                    responseObserver.onError(Status.INTERNAL
                            .withDescription("Nao foi possivel registar o pedido")
                            .withCause(e).asRuntimeException());
                }
            }
        };
    }

    @Override
    public void getStatus(GetStatusRequest request,
                          StreamObserver<GetStatusResponse> responseObserver) {
        try {
            Map<String, String> metadata = redis.getRequest(request.getExecutionId());
            if (metadata.isEmpty()) {
                responseObserver.onError(Status.NOT_FOUND
                        .withDescription("Pedido nao encontrado").asRuntimeException());
                return;
            }
            responseObserver.onNext(GetStatusResponse.newBuilder()
                    .setStatus(metadata.getOrDefault("state", "DESCONHECIDO")).build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Erro ao consultar estado")
                    .withCause(e).asRuntimeException());
        }
    }

    @Override
    public void getResult(GetResultRequest request,
                          StreamObserver<GetResultResponse> responseObserver) {
        String id = request.getExecutionId();
        try {
            Map<String, String> metadata = redis.getRequest(id);
            if (metadata.isEmpty()) {
                responseObserver.onError(Status.NOT_FOUND
                        .withDescription("Pedido nao encontrado").asRuntimeException());
                return;
            }

            String ownerIp = metadata.get("serverIp");
            int ownerPort = Integer.parseInt(metadata.get("serverPort"));

            if (serverIp.equals(ownerIp) && serverPort == ownerPort) {
                sendLocalResult(id, metadata.getOrDefault("state", "DESCONHECIDO"),
                        responseObserver);
            } else {
                forwardResult(id, ownerIp, ownerPort, responseObserver);
            }
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Erro ao consultar resultado")
                    .withCause(e).asRuntimeException());
        }
    }

    private synchronized void sendLocalResult(String id, String state,
            StreamObserver<GetResultResponse> out) throws Exception {
        if (!isFinished(state)) {
            out.onNext(GetResultResponse.newBuilder()
                    .setStatus(state).setFinished(false).build());
            out.onCompleted();
            return;
        }

        Path dir = requestManager.getRequestDirectory(id);
        Path stdout = dir.resolve("out.txt");
        Path stderr = dir.resolve("err.txt");

        try {
            sendFile(id, stdout, true, state, out);
            sendFile(id, stderr, false, state, out);
            // Marcador final explícito, inclusive quando stdout/stderr estão vazios.
            out.onNext(GetResultResponse.newBuilder()
                    .setStatus(state)
                    .setExitCode(readExitCode(dir))
                    .setFinished(true).build());
            out.onCompleted();
            requestManager.deleteDirectory(dir);
        } catch (Exception e) {
            out.onError(Status.INTERNAL.withDescription("Falha ao ler os ficheiros do resultado")
                    .withCause(e).asRuntimeException());
        }
    }

    private void sendFile(String id, Path file, boolean isStdout, String state,
                          StreamObserver<GetResultResponse> out) throws Exception {
        if (!Files.isRegularFile(file)) return;
        try (InputStream input = Files.newInputStream(file)) {
            byte[] buffer = new byte[CHUNK_SIZE];
            int count;
            while ((count = input.read(buffer)) != -1) {
                GetResultResponse.Builder chunk = GetResultResponse.newBuilder()
                        .setStatus(state)
                        .setExitCode(readExitCode(requestManager.getRequestDirectory(id)));
                ByteString bytes = ByteString.copyFrom(buffer, 0, count);
                if (isStdout) chunk.setStdout(bytes);
                else chunk.setStderr(bytes);
                out.onNext(chunk.build());
            }
        }
    }

    private void forwardResult(String id, String host, int port,
                               StreamObserver<GetResultResponse> out) {
        ManagedChannel channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext().build();
        try {
            RunServerServiceGrpc.newBlockingStub(channel)
                    .withDeadlineAfter(90, java.util.concurrent.TimeUnit.SECONDS)
                    .getResult(GetResultRequest.newBuilder().setExecutionId(id).build())
                    .forEachRemaining(out::onNext);
            out.onCompleted();
        } catch (Exception e) {
            out.onError(Status.UNAVAILABLE
                    .withDescription("Nao foi possivel contactar o RunServer proprietario")
                    .withCause(e).asRuntimeException());
        } finally {
            channel.shutdownNow();
        }
    }

    private int readExitCode(Path dir) {
        try {
            Path file = dir.resolve("exit.txt");
            return Files.exists(file) ? Integer.parseInt(Files.readString(file).trim()) : -1;
        } catch (Exception e) {
            return -1;
        }
    }

    private boolean isFinished(String state) {
        return "CONCLUIDO".equals(state) || "ERRO".equals(state)
                || "TIMEOUT".equals(state);
    }
}
