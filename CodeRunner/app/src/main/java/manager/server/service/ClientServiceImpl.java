package manager.server.service;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;

import manager.server.Endpoint;
import manager.server.Registry;
import servicestubs.managerclient.ProtoVoid;
import servicestubs.managerclient.ServerId;
import servicestubs.managerclient.ServiceGrpc;

public class ClientServiceImpl
        extends ServiceGrpc.ServiceImplBase {

    private final Registry registry;

    public ClientServiceImpl(Registry registry) {
        this.registry = registry;
    }

    @Override
    public void getServer(
            ProtoVoid req,
            StreamObserver<ServerId> obs) {

        try {
            Endpoint endpoint = registry.pick();

            if (endpoint == null) {
                obs.onError(
                        Status.UNAVAILABLE
                                .withDescription("Sem RunServers registados")
                                .asRuntimeException()
                );
                return;
            }

            obs.onNext(
                    ServerId.newBuilder()
                            .setIp(endpoint.ip())
                            .setPort(endpoint.port())
                            .build()
            );

            obs.onCompleted();

        } catch (Exception ex) {
            obs.onError(
                    Status.INTERNAL
                            .withDescription(String.valueOf(ex.getMessage()))
                            .asRuntimeException()
            );
        }
    }
}