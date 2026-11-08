
package grpcrunserverapp;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import servicestubs.managerserver.RedisId;

public class RunServer {

    public static void main(String[] args) {

        String serverIP = "127.0.0.1";
        int serverPort = 8000;

        String managerIP = "127.0.0.1";
        int managerPort = 9000;

        try {

            if (args.length == 4) {

                serverIP = args[0];
                serverPort = Integer.parseInt(args[1]);

                managerIP = args[2];
                managerPort = Integer.parseInt(args[3]);

            } else if (args.length != 0) {

                System.err.println(
                        "Uso: RunServer <serverIP> <serverPort> "
                                + "<managerIP> <managerPort>"
                );

                return;
            }

            RequestManager requestManager;
            RedisManager redis;
            try (ManagerRegistration registration =
                         new ManagerRegistration(managerIP, managerPort)) {

                RedisId redisId = registration.register(serverIP, serverPort);

                System.out.println("Redis: " +
                        redisId.getIp() + ":" + redisId.getPort());

                // A partir daqui, inicializas o RedisManager
                // e o RequestManager com os dados recebidos.

                // 2. Criar ligação ao Redis

                redis = new RedisManager(
                        redisId.getIp(),
                        redisId.getPort()
                );

                // 3. Criar gestores

                ContainerRunner runner = new ContainerRunner();

                requestManager = new RequestManager(
                        redis,
                        runner,
                        serverIP,
                        serverPort
                );
            }


            // 4. Criar servidor gRPC

            Server server = ServerBuilder
                    .forPort(serverPort)
                    .addService(
                            new ServiceClient(
                                    requestManager,
                                    redis,
                                    serverIP,
                                    serverPort
                            )
                    )
                    .build();

            server.start();

            System.out.println(
                    "RunServer iniciado em "
                            + serverIP + ":" + serverPort
            );

            Runtime.getRuntime().addShutdownHook(
                    new Thread(() -> {

                        System.out.println(
                                "A desligar RunServer..."
                        );

                        server.shutdown();

                        requestManager.close();
                        redis.close();

                    })
            );

            server.awaitTermination();

        } catch (Exception e) {

            System.err.println(
                    "Erro ao iniciar RunServer"
            );

            e.printStackTrace();
        }
    }
}
