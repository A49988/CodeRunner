package manager.server;

import java.util.ArrayList;
import java.util.List;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import servicestubs.managerclient.ProtoVoid;
import servicestubs.managerclient.ServiceGrpc;
import servicestubs.managerserver.RedisId;
import servicestubs.managerserver.RegisterServiceGrpc;

public class App {

    record Endpoint(String ip, int port) {}

    static class Registry {
        private final List<Endpoint> servers = new ArrayList<>();
        private int next = 0;

        synchronized void add(Endpoint e) {
            if (!servers.contains(e)) servers.add(e);
        }

        synchronized Endpoint pick() {
            if (servers.isEmpty()) return null;
            Endpoint e = servers.get(next % servers.size());
            next = (next + 1) % servers.size();
            return e;
        }
    }

    // Contrato a: RunServer -> ManagerServer
    static class RegisterImpl extends RegisterServiceGrpc.RegisterServiceImplBase {
        private final Registry registry;
        private final String redisIp;
        private final int redisPort;

        RegisterImpl(Registry r, String redisIp, int redisPort) {
            this.registry = r;
            this.redisIp = redisIp;
            this.redisPort = redisPort;
        }

        @Override
        public void register(servicestubs.managerserver.ServerId req, StreamObserver<RedisId> obs) {
            try {
                registry.add(new Endpoint(req.getIp(), req.getPort()));
                System.out.println("Registado: " + req.getIp() + ":" + req.getPort());
                obs.onNext(RedisId.newBuilder().setIp(redisIp).setPort(redisPort).build());
                obs.onCompleted();
            } catch (Exception e) {
                obs.onError(Status.INTERNAL.withDescription(String.valueOf(e.getMessage())).asRuntimeException());
            }
        }
    }

    // Contrato b: Client -> ManagerServer
    static class ClientImpl extends ServiceGrpc.ServiceImplBase {
        private final Registry registry;

        ClientImpl(Registry r) { this.registry = r; }

        @Override
        public void getServer(ProtoVoid req, StreamObserver<servicestubs.managerclient.ServerId> obs) {
            try {
                Endpoint e = registry.pick();
                if (e == null) {
                    obs.onError(Status.UNAVAILABLE.withDescription("Sem RunServers registados").asRuntimeException());
                    return;
                }
                obs.onNext(servicestubs.managerclient.ServerId.newBuilder()
                        .setIp(e.ip()).setPort(e.port()).build());
                obs.onCompleted();
            } catch (Exception ex) {
                obs.onError(Status.INTERNAL.withDescription(String.valueOf(ex.getMessage())).asRuntimeException());
            }
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 3) {
            System.err.println("Uso: App <portoManager> <redisIp> <redisPorto>");
            return;
        }
        int port = Integer.parseInt(args[0]);
        Registry registry = new Registry();

        Server server = ServerBuilder.forPort(port)
                .addService(new RegisterImpl(registry, args[1], Integer.parseInt(args[2])))
                .addService(new ClientImpl(registry))
                .build()
                .start();

        System.out.println("ManagerServer a escutar no porto " + port);
        server.awaitTermination();
    }
}