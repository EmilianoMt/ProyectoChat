package src.Server;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class HiloChatServer implements Runnable {
    private Socket socket;
    private final Vector<Socket> vector;
    private DataInputStream netIn;
    private DataOutputStream netOut;
    private static List<String> usuarios = new ArrayList<>(); // Lista de usuarios conectados
    private String username;

    public HiloChatServer(Socket socket, Vector<Socket> vector) {
        this.socket = socket;
        this.vector = vector;
    }

    private void initStreams() throws IOException {
        netIn = new DataInputStream(socket.getInputStream());
        username = netIn.readUTF(); // Leer el nombre de usuario al iniciar la conexión
        synchronized (usuarios) {
            usuarios.add(username); // Agregar usuario a la lista
        }
    }

    private void sendMsgToAll(String msg) {
        synchronized (vector) {
            for (Socket soc : vector) {
                try {
                    netOut = new DataOutputStream(soc.getOutputStream());
                    netOut.writeUTF(msg);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void sendUserListToAll() {
        synchronized (vector) {
            String userList = "USERS:" + String.join(",", usuarios);
            for (Socket soc : vector) {
                try {
                    netOut = new DataOutputStream(soc.getOutputStream());
                    netOut.writeUTF(userList); // Enviar la lista de usuarios conectados
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void run() {
        try {
            initStreams();
            sendMsgToAll("Server: " + username + " se unió al chat.");
            sendUserListToAll(); // Enviar la lista de usuarios a todos

            while (true) {
                String msg = netIn.readUTF();
                sendMsgToAll(msg);
            }
        } catch (IOException ioe) {
            System.out.println("Client disconnected");
            try {
                synchronized (usuarios) {
                    usuarios.remove(username); // Remover usuario de la lista
                }
                synchronized (vector) {
                    vector.remove(socket);
                }
                sendUserListToAll(); // Enviar la lista actualizada a todos
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } finally {
            closeResources();
        }
    }

    private void closeResources() {
        try {
            if (netIn != null) netIn.close();
            if (netOut != null) netOut.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
