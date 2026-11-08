package manager.server;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import manager.server.service.ClientServiceImpl;
import manager.server.service.RegisterServiceImpl;

public class App {

    public static void main(String[] args) throws Exception {

        if (args.length != 3) {
            System.err.println(
                    "Uso: App <portoManager> <redisIp> <redisPorto>"
            );
            return;
        }

        int port = Integer.parseInt(args[0]);
        String redisIp = args[1];
        int redisPort = Integer.parseInt(args[2]);

        Registry registry = new Registry();

        Server server = ServerBuilder.forPort(port)
                .addService(
                        new RegisterServiceImpl(registry, redisIp, redisPort)
                )
                .addService(
                        new ClientServiceImpl(registry)
                )
                .build()
                .start();

        System.out.println(
                "ManagerServer a escutar no porto " + port
        );

        server.awaitTermination();
    }
}