
package coderunner.runserver;

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

public class ServiceClient extends RunServerServiceGrpc.RunServerServiceImplBase {

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
    // SUBMIT PROGRAM
    // ------------------------------------------------

    @Override
    public StreamObserver<SubmitProgramRequest> submitProgram(
            StreamObserver<SubmitProgramResponse> responseObserver) {

        RequestData data = new RequestData();

        return new StreamObserver<>() {

            private boolean failed = false;

            @Override
            public void onNext(SubmitProgramRequest request) {

                if (failed) return;

                try {
                    byte[] program = request.getData().toByteArray();
                    byte[] stdin = request.getStdin().toByteArray();

                    if (program.length > 0) {
                        data.addProgram(program);
                    }

                    if (stdin.length > 0) {
                        data.addStdin(stdin);
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
                        "Erro durante a submissão: " + throwable.getMessage()
                );
            }

            @Override
            public void onCompleted() {

                if (failed) return;

                try {
                    String id = requestManager.submit(data);

                    SubmitProgramResponse response =
                            SubmitProgramResponse.newBuilder()
                                    .setExecutionId(id)
                                    .build();

                    responseObserver.onNext(response);
                    responseObserver.onCompleted();

                    System.out.println("Pedido recebido: " + id);

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
                                    .withDescription("Erro ao submeter o pedido")
                                    .asRuntimeException()
                    );
                }
            }
        };
    }

    // ------------------------------------------------
    // GET STATUS
    // ------------------------------------------------

    @Override
    public void getStatus(
            GetStatusRequest request,
            StreamObserver<GetStatusResponse> responseObserver) {

        try {
            Map<String, String> data =
                    redis.getRequest(request.getExecutionId());

            if (data.isEmpty()) {
                responseObserver.onError(
                        Status.NOT_FOUND
                                .withDescription("Pedido não encontrado")
                                .asRuntimeException()
                );
                return;
            }

            String executorIp = data.get("serverIp");
            int executorPort = Integer.parseInt(data.get("serverPort"));

            boolean isLocal =
                    serverIp.equals(executorIp)
                            && serverPort == executorPort;

            if (!isLocal) {
                forwardStatus(
                        request.getExecutionId(),
                        executorIp,
                        executorPort,
                        responseObserver
                );
                return;
            }

            GetStatusResponse response =
                    GetStatusResponse.newBuilder()
                            .setStatus(data.get("state"))
                            .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            e.printStackTrace();

            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription("Erro ao consultar o estado")
                            .asRuntimeException()
            );
        }
    }

    private void forwardStatus(
            String id,
            String executorIp,
            int executorPort,
            StreamObserver<GetStatusResponse> responseObserver) {

        ManagedChannel channel = ManagedChannelBuilder
                .forAddress(executorIp, executorPort)
                .usePlaintext()
                .build();

        try {
            GetStatusRequest request = GetStatusRequest.newBuilder()
                    .setExecutionId(id)
                    .build();

            GetStatusResponse response =
                    RunServerServiceGrpc.newBlockingStub(channel)
                            .getStatus(request);

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            responseObserver.onError(
                    Status.UNAVAILABLE
                            .withDescription("Não foi possível contactar o RunServer executor")
                            .withCause(e)
                            .asRuntimeException()
            );
        } finally {
            channel.shutdownNow();
        }
    }

    // ------------------------------------------------
    // GET RESULT
    // ------------------------------------------------

    @Override
    public void getResult(
            GetResultRequest request,
            StreamObserver<GetResultResponse> responseObserver) {

        String id = request.getExecutionId();

        try {
            Map<String, String> data = redis.getRequest(id);

            if (data.isEmpty()) {
                responseObserver.onError(
                        Status.NOT_FOUND
                                .withDescription("Pedido não encontrado")
                                .asRuntimeException()
                );
                return;
            }

            String executorIp = data.get("serverIp");
            int executorPort = Integer.parseInt(data.get("serverPort"));

            boolean isLocal =
                    serverIp.equals(executorIp)
                            && serverPort == executorPort;

            if (isLocal) {
                sendLocalResult(id, data, responseObserver);
            } else {
                forwardResult(id, executorIp, executorPort, responseObserver);
            }

        } catch (Exception e) {
            e.printStackTrace();

            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription("Erro ao consultar o resultado")
                            .asRuntimeException()
            );
        }
    }

    // ------------------------------------------------
    // RESULTADO LOCAL
    // ------------------------------------------------

    private void sendLocalResult(
            String id,
            Map<String, String> data,
            StreamObserver<GetResultResponse> responseObserver)
            throws IOException {

        String state = data.get("state");

        if (!isFinished(state)) {
            sendResultState(state, -1, false, responseObserver);
            responseObserver.onCompleted();
            return;
        }

        Path dir = requestManager.getRequestDirectory(id);

        Path stdout = dir.resolve("out.txt");
        Path stderr = dir.resolve("err.txt");
        Path exitFile = dir.resolve("exit.txt");

        int exitCode = -1;

        if (Files.exists(exitFile)) {
            exitCode = Integer.parseInt(
                    Files.readString(exitFile).trim()
            );
        }

        boolean sentOutput = false;

        try {
            sentOutput |= sendFile(
                    stdout, true, state, exitCode, responseObserver
            );

            sentOutput |= sendFile(
                    stderr, false, state, exitCode, responseObserver
            );

            if (!sentOutput) {
                sendResultState(state, exitCode, true, responseObserver);
            } else {
                sendResultState(state, exitCode, true, responseObserver);
            }

            responseObserver.onCompleted();

            requestManager.deleteDirectory(dir);

        } catch (Exception e) {
            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription("Erro ao ler os ficheiros do resultado")
                            .asRuntimeException()
            );
        }
    }

    private boolean sendFile(
            Path file,
            boolean isStdout,
            String state,
            int exitCode,
            StreamObserver<GetResultResponse> responseObserver)
            throws IOException {

        if (!Files.exists(file)) return false;

        boolean sent = false;

        try (InputStream input = Files.newInputStream(file)) {

            byte[] buffer = new byte[CHUNK_SIZE];
            int bytesRead;

            while ((bytesRead = input.read(buffer)) != -1) {

                GetResultResponse.Builder chunk =
                        GetResultResponse.newBuilder()
                                .setStatus(state)
                                .setExitCode(exitCode)
                                .setFinished(false);

                ByteString content =
                        ByteString.copyFrom(buffer, 0, bytesRead);

                if (isStdout) {
                    chunk.setStdout(content);
                } else {
                    chunk.setStderr(content);
                }

                responseObserver.onNext(chunk.build());
                sent = true;
            }
        }

        return sent;
    }

    private void sendResultState(
            String state,
            int exitCode,
            boolean finished,
            StreamObserver<GetResultResponse> responseObserver) {

        GetResultResponse response = GetResultResponse.newBuilder()
                .setStatus(state)
                .setExitCode(exitCode)
                .setFinished(finished)
                .build();

        responseObserver.onNext(response);
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
            StreamObserver<GetResultResponse> responseObserver) {

        ManagedChannel channel = ManagedChannelBuilder
                .forAddress(executorIp, executorPort)
                .usePlaintext()
                .build();

        try {
            GetResultRequest request = GetResultRequest.newBuilder()
                    .setExecutionId(id)
                    .build();

            RunServerServiceGrpc.newBlockingStub(channel)
                    .getResult(request)
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
