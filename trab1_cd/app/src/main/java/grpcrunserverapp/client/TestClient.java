package grpcrunserverapp.client;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import servicestubs.managerclient.ProtoVoid;
import servicestubs.managerclient.ServiceGrpc;
import servicestubs.managerserver.RegisterServiceGrpc;

public class TestClient {
    public static void main(String[] args) {
        ManagedChannel ch = ManagedChannelBuilder.forAddress("localhost", 50051).usePlaintext().build();
        var reg = RegisterServiceGrpc.newBlockingStub(ch);
        var cli = ServiceGrpc.newBlockingStub(ch);

        for (int p = 6001; p <= 6003; p++) {
            var redis = reg.register(servicestubs.managerserver.ServerId.newBuilder()
                    .setIp("127.0.0.1").setPort(p).build());
            System.out.println("Redis: " + redis.getIp() + ":" + redis.getPort());
        }
        for (int i = 0; i < 6; i++) {
            var s = cli.getServer(ProtoVoid.getDefaultInstance());
            System.out.println("Servidor: " + s.getIp() + ":" + s.getPort());
        }
        ch.shutdown();
    }
}