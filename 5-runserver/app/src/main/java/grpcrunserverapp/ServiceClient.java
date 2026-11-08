package grpcrunserverapp;

import com.google.protobuf.ByteString;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;

import servicestubs.runserver.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class ServiceClient extends RunServiceGrpc.RunServiceImplBase {

    private static final int CHUNK_SIZE = 64 * 1024;

    private final RequestManager requestManager;
    private final RedisManager redis;

    private final String serverIp;
    private final int serverPort;

    public ServiceClient(
            RequestManager requestManager,
            RedisManager redis,
            String serverIp,
            int serverPort) {

        this.requestManager = requestManager;
        this.redis = redis;
        this.serverIp = serverIp;
        this.serverPort = serverPort;
    }

    // ------------------------------------------------
    // SUBMETER
    // ------------------------------------------------

    @Override
    public StreamObserver<SubmissionChunk> submeter(
            StreamObserver<RequestId> responseObserver) {

        RequestData data = new RequestData();

        return new StreamObserver<>() {

            private boolean failed = false;

            @Override
            public void onNext(SubmissionChunk chunk) {

                if (failed) {
                    return;
                }

                try {
                    byte[] bytes = chunk.getData().toByteArray();

                    switch (chunk.getType()) {

                        case PROGRAM:
                            data.addProgram(bytes);
                            break;

                        case STDIN:
                            data.addStdin(bytes);
                            break;

                        default:
                            throw new IllegalArgumentException(
                                    "Tipo de bloco inválido"
                            );
                    }

                } catch (Exception e) {
                    failed = true;

                    responseObserver.onError(
                            Status.INVALID_ARGUMENT
                                    .withDescription(e.getMessage())
                                    .asRuntimeException()
                    );
                }
            }

            @Override
            public void onError(Throwable throwable) {
                System.err.println(
                        "Erro durante a submissão: "
                                + throwable.getMessage()
                );
            }

            @Override
            public void onCompleted() {

                if (failed) {
                    return;
                }

                try {

                    String id = requestManager.submit(data);

                    RequestId requestId = RequestId.newBuilder()
                            .setId(id)
                            .build();

                    responseObserver.onNext(requestId);
                    responseObserver.onCompleted();

                    System.out.println(
                            "Pedido recebido: " + id
                    );

                } catch (IllegalArgumentException e) {

                    responseObserver.onError(
                            Status.INVALID_ARGUMENT
                                    .withDescription(e.getMessage())
                                    .asRuntimeException()
                    );

                } catch (Exception e) {

                    e.printStackTrace();

                    responseObserver.onError(
                            Status.INTERNAL
                                    .withDescription(
                                            "Erro ao submeter o pedido"
                                    )
                                    .asRuntimeException()
                    );
                }
            }
        };
    }

    // ------------------------------------------------
    // GETRESULT
    // ------------------------------------------------

    @Override
    public void getresult(
            RequestId requestId,
            StreamObserver<ResultChunk> responseObserver) {

        String id = requestId.getId();

        try {

            Map<String, String> request = redis.getRequest(id);

            if (request.isEmpty()) {

                responseObserver.onError(
                        Status.NOT_FOUND
                                .withDescription("Pedido não encontrado")
                                .asRuntimeException()
                );

                return;
            }

            String executorIp = request.get("serverIp");

            int executorPort = Integer.parseInt(
                    request.get("serverPort")
            );

            // Verificar se este RunServer é o executor.

            boolean isLocal =
                    serverIp.equals(executorIp)
                            && serverPort == executorPort;

            if (isLocal) {

                sendLocalResult(id, request, responseObserver);

            } else {

                forwardResult(
                        id,
                        executorIp,
                        executorPort,
                        responseObserver
                );
            }

        } catch (Exception e) {

            e.printStackTrace();

            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription(
                                    "Erro ao consultar o resultado"
                            )
                            .asRuntimeException()
            );
        }
    }

    // ------------------------------------------------
    // RESULTADO LOCAL
    // ------------------------------------------------

    private void sendLocalResult(
            String id,
            Map<String, String> request,
            StreamObserver<ResultChunk> responseObserver)
            throws IOException {

        String state = request.get("state");

        // Se ainda está a executar, enviar apenas o estado.

        if (!isFinished(state)) {

            sendState(state, responseObserver);
            responseObserver.onCompleted();

            return;
        }

        Path dir = requestManager.getRequestDirectory(id);

        Path stdout = dir.resolve("out.txt");
        Path stderr = dir.resolve("err.txt");

        boolean sentOutput = false;

        try {

            sentOutput |= sendFile(
                    stdout,
                    true,
                    state,
                    responseObserver
            );

            sentOutput |= sendFile(
                    stderr,
                    false,
                    state,
                    responseObserver
            );

            // Mesmo sem output, enviar o estado final.

            if (!sentOutput) {
                sendState(state, responseObserver);
            }

            responseObserver.onCompleted();

            // Os resultados são locais e removidos após o consumo.
            requestManager.deleteDirectory(dir);

        } catch (Exception e) {

            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription(
                                    "Erro ao ler os ficheiros do resultado"
                            )
                            .asRuntimeException()
            );
        }
    }

    private boolean sendFile(
            Path file,
            boolean isStdout,
            String state,
            StreamObserver<ResultChunk> responseObserver)
            throws IOException {

        if (!Files.exists(file)) {
            return false;
        }

        boolean sent = false;

        try (InputStream input = Files.newInputStream(file)) {

            byte[] buffer = new byte[CHUNK_SIZE];

            int bytesRead;

            while ((bytesRead = input.read(buffer)) != -1) {

                ByteString content = ByteString.copyFrom(
                        buffer, 0, bytesRead
                );

                ResultChunk.Builder chunk = ResultChunk.newBuilder()
                        .setState(state)
                        .setServerIp(serverIp)
                        .setServerPort(serverPort);

                if (isStdout) {
                    chunk.setResult(content);
                } else {
                    chunk.setError(content);
                }

                responseObserver.onNext(chunk.build());

                sent = true;
            }
        }

        return sent;
    }

    private void sendState(
            String state,
            StreamObserver<ResultChunk> responseObserver) {

        ResultChunk chunk = ResultChunk.newBuilder()
                .setState(state)
                .setServerIp(serverIp)
                .setServerPort(serverPort)
                .build();

        responseObserver.onNext(chunk);
    }

    private boolean isFinished(String state) {

        return "CONCLUIDO".equals(state)
                || "ERRO".equals(state)
                || "TIMEOUT".equals(state);
    }

    // ------------------------------------------------
    // ENCAMINHAMENTO PARA OUTRO RUNSERVER
    // ------------------------------------------------

    private void forwardResult(
            String id,
            String executorIp,
            int executorPort,
            StreamObserver<ResultChunk> responseObserver) {

        ManagedChannel channel = ManagedChannelBuilder
                .forAddress(executorIp, executorPort)
                .usePlaintext()
                .build();

        try {

            RunServiceGrpc.RunServiceBlockingStub stub =
                    RunServiceGrpc.newBlockingStub(channel);

            RequestId requestId = RequestId.newBuilder()
                    .setId(id)
                    .build();

            stub.getresult(requestId)
                    .forEachRemaining(responseObserver::onNext);

            responseObserver.onCompleted();

        } catch (Exception e) {

            responseObserver.onError(
                    Status.UNAVAILABLE
                            .withDescription(
                                    "Não foi possível contactar o RunServer executor"
                            )
                            .withCause(e)
                            .asRuntimeException()
            );

        } finally {

            channel.shutdownNow();
        }
    }
}