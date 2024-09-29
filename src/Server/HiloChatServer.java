package src.server;

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
    private static List<String> usuarios = new ArrayList<>();
    private String username;

    public HiloChatServer(Socket socket, Vector<Socket> vector) {
        this.socket = socket;
        this.vector = vector;
    }

    public static List<String> getUsuarios() {
        return usuarios;
    }

    private void initStreams() throws IOException {
        netIn = new DataInputStream(socket.getInputStream());
        username = netIn.readUTF();
        synchronized (usuarios) {
            usuarios.add(username);
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
                    netOut.writeUTF(userList);
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
            sendUserListToAll();

            while (true) {
                String msg = netIn.readUTF();
                sendMsgToAll(msg);
            }
        } catch (IOException ioe) {
            System.out.println("Client disconnected");
            try {
                synchronized (usuarios) {
                    usuarios.remove(username);
                }
                synchronized (vector) {
                    vector.remove(socket);
                }
                sendUserListToAll();
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
