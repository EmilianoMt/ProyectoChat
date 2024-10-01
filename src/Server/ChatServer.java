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
    private static final int port = 8081;
    private static final Vector<Socket> vector = new Vector<>();

    public static void main(String[] args) {
        try (ServerSocket sSocket = new ServerSocket(port)) {
            ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
            scheduler.scheduleAtFixedRate(() -> {
                synchronized (vector) {
                    vector.removeIf(soc -> {
                        if (soc.isClosed()) {
                            return true;
                        }
                        try {
                            DataOutputStream netOut = new DataOutputStream(soc.getOutputStream());
                            String userList = "USERS:" + String.join(",", HiloChatServer.getUsuarios());
                            netOut.writeUTF(userList);
                        } catch (IOException e) {
                            e.printStackTrace();
                            return true;
                        }
                        return false;
                    });
                }
            }, 0, 30, TimeUnit.SECONDS);

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