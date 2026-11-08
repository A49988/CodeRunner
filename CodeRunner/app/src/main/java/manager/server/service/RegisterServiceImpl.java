package manager.server.service;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;

import manager.server.Endpoint;
import manager.server.Registry;
import servicestubs.managerserver.RedisId;
import servicestubs.managerserver.RegisterServiceGrpc;
import servicestubs.managerserver.ServerId;

public class RegisterServiceImpl
        extends RegisterServiceGrpc.RegisterServiceImplBase {

    private final Registry registry;
    private final String redisIp;
    private final int redisPort;

    public RegisterServiceImpl(
            Registry registry,
            String redisIp,
            int redisPort) {

        this.registry = registry;
        this.redisIp = redisIp;
        this.redisPort = redisPort;
    }

    @Override
    public void register(ServerId req, StreamObserver<RedisId> obs) {
        try {
            registry.add(new Endpoint(req.getIp(), req.getPort()));

            System.out.println(
                    "Registado: " + req.getIp() + ":" + req.getPort()
            );

            obs.onNext(
                    RedisId.newBuilder()
                            .setIp(redisIp)
                            .setPort(redisPort)
                            .build()
            );

            obs.onCompleted();

        } catch (Exception e) {
            obs.onError(
                    Status.INTERNAL
                            .withDescription(String.valueOf(e.getMessage()))
                            .asRuntimeException()
            );
        }
    }
}