package src.Server;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ChatServer {
    private static final int port = 8081; // Puerto en el que el servidor escuchará las conexiones.
    private static final Vector<Socket> vector = new Vector<>(); // Vector que almacena las conexiones de los clientes.

    public static void main(String[] args) {
        try (ServerSocket sSocket = new ServerSocket(port)) { // Crea un ServerSocket que escucha en el puerto especificado.
            ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1); // Crea un scheduler con un solo hilo.
            scheduler.scheduleAtFixedRate(() -> { // Programa una tarea que se ejecuta periódicamente.
                synchronized (vector) { // Sincroniza el acceso al vector.
                    vector.removeIf(soc -> { // Elimina los sockets cerrados del vector.
                        if (soc.isClosed()) {
                            return true; // Si el socket está cerrado, se elimina del vector.
                        }
                        try {
                            DataOutputStream netOut = new DataOutputStream(soc.getOutputStream()); // Crea un DataOutputStream para enviar datos al cliente.
                            String userList = "USERS:" + String.join(",", HiloChatServer.getUsuarios()); // Obtiene la lista de usuarios conectados.
                            netOut.writeUTF(userList); // Envía la lista de usuarios al cliente.
                        } catch (IOException e) {
                            e.printStackTrace();
                            return true; // Si ocurre una excepción, se elimina el socket del vector.
                        }
                        return false; // Si no hay problemas, el socket permanece en el vector.
                    });
                }
            }, 0, 30, TimeUnit.SECONDS); // La tarea se ejecuta cada 30 segundos.

            // Bucle principal que acepta nuevas conexiones de clientes.
            while (true) {
                try {
                    System.out.println("Chat server abierto y esperando conexiones en el puerto: " + port);
                    Socket socket = sSocket.accept(); // Acepta una nueva conexión.
                    synchronized (vector) {
                        vector.add(socket); // Añade la nueva conexión al vector.
                    }
                    Thread hilo = new Thread(new HiloChatServer(socket, vector)); // Crea un nuevo hilo para gestionar el cliente.
                    hilo.start(); // Inicia el hilo.
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}