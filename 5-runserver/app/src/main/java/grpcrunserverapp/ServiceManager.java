package grpcrunserverapp;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import servicestubs.managerserver.*;

public class ServiceManager {

    private final String managerIP;
    private final int managerPort;

    private String redisIP;
    private int redisPort;

    public ServiceManager(String ip, int port) {
        this.managerIP = ip;
        this.managerPort = port;
    }

    public void register(String serverIP, int serverPort) {

        ManagedChannel channel = ManagedChannelBuilder
                .forAddress(managerIP, managerPort)
                .usePlaintext()
                .build();

        try {
            RegisterServiceGrpc.RegisterServiceBlockingStub stub =
                    RegisterServiceGrpc.newBlockingStub(channel);

            ServerId server = ServerId.newBuilder()
                    .setIp(serverIP)
                    .setPort(serverPort)
                    .build();

            RedisId redis = stub.register(server);

            this.redisIP = redis.getIp();
            this.redisPort = redis.getPort();

            System.out.println("RunServer registado!");
            System.out.println("Redis: " + redisIP + ":" + redisPort);

        } finally {
            channel.shutdown();
        }
    }

    public String getRedisIP() {
        return redisIP;
    }

    public int getRedisPort() {
        return redisPort;
    }
}