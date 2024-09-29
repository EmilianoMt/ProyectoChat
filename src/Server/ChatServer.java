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
    // Vector que almacena las conexiones de los clientes.
    private Vector<Socket> vector = new Vector<>();
    private int port; // Puerto en el que el server escucha las conexiones.

    // Constructor que recibe el puerto en el que se ejecutará el servidor.
    public ChatServer(int port) {
        this.port = port;
    }

    // Método para establecer la conexión del servidor en el puerto especificado.
    private ServerSocket connect() {
        try {
            ServerSocket sSocket = new ServerSocket(port);
            return sSocket; 
        } catch (IOException ioe) {
            System.out.println("No se puede realizar la conexión en el puerto " + port);
        }
        return null; 
    }

    // Método principal que inicia el servidor y acepta conexiones de clientes.
    public void principal() {
        ServerSocket sSocket = connect(); // Intenta conectar el servidor.
        if (sSocket != null) {
            // Funcion que envia la lista de usuarios cada 30 segundos.
            ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
            scheduler.scheduleAtFixedRate(() -> {
                synchronized (vector) { // Sincroniza el acceso al vector de conexiones.
                    for (Socket soc : vector) { // Itera sobre cada conexión activa.
                        try {
                            DataOutputStream netOut = new DataOutputStream(soc.getOutputStream());
                            // Genera la lista de usuarios conectados.
                            String userList = "USERS:" + String.join(",", HiloChatServer.getUsuarios());
                            netOut.writeUTF(userList); // Envía la lista de usuarios a cada cliente.
                        } catch (IOException e) {
                            e.printStackTrace(); 
                        }
                    }
                }
            }, 0, 30, TimeUnit.SECONDS); // Repeticion cada 30 segundos.

            // Bucle principal que acepta nuevas conexiones de clientes.
            while (true) {
                try {
                    System.out.println("Chat server abierto y esperando conexiones en el puerto: " + port);
                    Socket socket = sSocket.accept(); // Acepta una nueva conexión.
                    vector.add(socket); // Añade la nueva conexión al vector.
                    Thread hilo = new Thread(new HiloChatServer(socket, vector)); // Crea un nuevo hilo para gestionar el cliente.
                    hilo.start(); // Inicia el hilo.
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            }
        } else {
            System.err.println("No se puede iniciar el servidor."); // Mensaje de error si no se puede conectar.
        }
    }

    // Método principal que crea una instancia del servidor de chat en el puerto 8081.
    public static void main(String[] args) {
        ChatServer chat = new ChatServer(8081);
        chat.principal(); // Inicia el servidor.
    }
}
