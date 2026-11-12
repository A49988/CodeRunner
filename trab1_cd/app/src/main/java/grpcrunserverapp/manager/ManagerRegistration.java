package grpcrunserverapp.manager;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import servicestubs.managerserver.RedisId;
import servicestubs.managerserver.RegisterServiceGrpc;
import servicestubs.managerserver.ServerId;

public class ManagerRegistration implements AutoCloseable {

    private final ManagedChannel channel;
    private final RegisterServiceGrpc.RegisterServiceBlockingStub stub;

    public ManagerRegistration(String managerIp, int managerPort) {
        this.channel = ManagedChannelBuilder
                .forAddress(managerIp, managerPort)
                .usePlaintext()
                .build();

        this.stub = RegisterServiceGrpc.newBlockingStub(channel);
    }

    public RedisId register(String runServerIp, int runServerPort) {
        ServerId serverId = ServerId.newBuilder()
                .setIp(runServerIp)
                .setPort(runServerPort)
                .build();

        return stub.register(serverId);
    }

    @Override
    public void close() {
        channel.shutdown();
    }
}