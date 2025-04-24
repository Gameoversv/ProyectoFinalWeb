package servicios;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import URLSnap.app.MainApp;

public class GrpcServer {
    public static void main(String[] args) throws Exception {
        // ✅ Inicializamos MongoDB y DAOs desde MainApp
        MainApp.initDAOs();

        Server server = ServerBuilder
                .forPort(9090)
                .addService(new UrlServiceImpl()) // Implementación del servicio
                .build()
                .start();

        System.out.println("🚀 Servidor gRPC iniciado en el puerto 9090...");
        server.awaitTermination();
    }
}
