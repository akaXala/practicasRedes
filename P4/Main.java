import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class Main {
    // Configuraciones
    ServerSocket server;
    public static int PORT = 8080;

    // Tamaño de la thread pool
    private static final int THREAD_POOL_SIZE = 3;
    private ThreadPoolExecutor threadPool;
    
    // Varibale para mostrar el funcionamiento de la pool
    public static final boolean DEBUG_SLEEP = false;

    public Main() throws IOException {
        System.out.println("Iniciando servidor...");

        // Creamos un nuevo servido
        server = new ServerSocket(PORT);
        System.out.println("Servidor iniciado.");
        System.out.println("Esperando conexión");

        // Inicializamos la thread pool
        threadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(THREAD_POOL_SIZE);

        while (true) {
            // Aceptamos cualquier conexión
            Socket accept = server.accept();

            // Creamos nuestro objeto servidor
            try {
                threadPool.execute(new Servidor(accept));
                System.out.println("Cliente conectado " + accept.getInetAddress());
                System.out.println("En el puerto " + accept.getPort());
                System.out.println("Hilos activos: " + threadPool.getActiveCount() + " | En cola: " + threadPool.getQueue().size() + " | Maximo permitido: " + threadPool.getMaximumPoolSize());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
    }

    public static void main(String[] args) throws IOException {
        new Main();
    }
}
