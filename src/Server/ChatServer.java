package src.server;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ChatServer {
    private Vector<Socket> vector = new Vector<>();
    private int port;

    public ChatServer(int port) {
        this.port = port;
    }

    private ServerSocket connect() {
        try {
            ServerSocket sSocket = new ServerSocket(port);
            return sSocket;
        } catch (IOException ioe) {
            System.out.println("No se puede realizar la conexión en el puerto " + port);
        }
        return null;
    }

    public void principal() {
        ServerSocket sSocket = connect();
        if (sSocket != null) {
            ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
            scheduler.scheduleAtFixedRate(() -> {
                synchronized (vector) {
                    for (Socket soc : vector) {
                        try {
                            DataOutputStream netOut = new DataOutputStream(soc.getOutputStream());
                            String userList = "USERS:" + String.join(",", HiloChatServer.getUsuarios());
                            netOut.writeUTF(userList);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }, 0, 30, TimeUnit.SECONDS);

            while (true) {
                try {
                    System.out.println("Chat server abierto y esperando conexiones en el puerto: " + port);
                    Socket socket = sSocket.accept();
                    vector.add(socket);
                    Thread hilo = new Thread(new HiloChatServer(socket, vector));
                    hilo.start();
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            }
        } else {
            System.err.println("No se puede iniciar el servidor.");
        }
    }

    public static void main(String[] args) {
        ChatServer chat = new ChatServer(8081);
        chat.principal();
    }
}
