package src.Server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Vector;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

public class HiloChatServer implements Runnable {
    private Socket socket;
    private final Vector<Socket> vector;
    private DataInputStream netIn;
    private DataOutputStream netOut;
    private static List<String> usuarios = new ArrayList<>();
    private String username;

    // Constructor
    public HiloChatServer(Socket socket, Vector<Socket> vector) {
        this.socket = socket;
        this.vector = vector;
    }

    public static List<String> getUsuarios() {
        return usuarios;
    }

    private void initStreams() throws IOException {
        netIn = new DataInputStream(socket.getInputStream());
        netOut = new DataOutputStream(socket.getOutputStream());
        username = netIn.readUTF();
        synchronized (usuarios) {
            usuarios.add(username);
        }
    }

    private void sendMsgToAll(String msg) {
        synchronized (vector) {
            for (Socket soc : vector) {
                if (!soc.isClosed() && soc != socket) { // Evitar enviar al remitente dos veces
                    try {
                        DataOutputStream out = new DataOutputStream(soc.getOutputStream());
                        out.writeUTF(msg);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private void sendUserListToAll() {
        synchronized (vector) {
            String userList = "USERS:" + String.join(",", usuarios);
            for (Socket soc : vector) {
                if (!soc.isClosed()) {
                    try {
                        DataOutputStream out = new DataOutputStream(soc.getOutputStream());
                        out.writeUTF(userList);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    @Override
    public void run() {
        try {
            initStreams();
            synchronized (vector) {
                vector.add(socket);
            }
            sendMsgToAll("Server: " + username + " se unió al chat.");
            sendUserListToAll();

            while (true) {
                try {
                    String msg = netIn.readUTF();
                    if (msg.startsWith("PRIVATE:")) {
                        handlePrivateMessage(msg);
                    } else if (msg.startsWith("REQUEST_PRIVATE_KEY:")) {
                        handlePrivateKeyRequest(msg); // Manejar solicitud de clave
                    } else {
                        sendMsgToAll(msg);
                    }
                } catch (IOException ioe) {
                    System.out.println("Client disconnected: " + username);
                    break;
                }
            }
        } catch (IOException ioe) {
            System.out.println("Error initializing streams for: " + username);
        } finally {
            synchronized (usuarios) {
                usuarios.remove(username);
            }
            synchronized (vector) {
                vector.remove(socket);
                sendUserListToAll();
            }
            closeResources();
        }
    }

    private void handlePrivateMessage(String msg) {
        String[] parts = msg.split(":", 3);
        String recipient = parts[1];
        String encryptedMessage = parts[2];

        synchronized (vector) {
            for (Socket soc : vector) {
                try {
                    DataOutputStream out = new DataOutputStream(soc.getOutputStream());
                    // Envía el mensaje solo al destinatario
                    if (usuarios.contains(recipient)) {
                        out.writeUTF("PRIVATE:" + username + ":" + encryptedMessage);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // Método para manejar la solicitud de clave privada
    private void handlePrivateKeyRequest(String msg) {
        String[] parts = msg.split(":", 2);
        String recipient = parts[1];

        try {
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(128); // Tamaño de la clave
            SecretKey secretKey = keyGen.generateKey();
            String encodedKey = Base64.getEncoder().encodeToString(secretKey.getEncoded());

            // Enviar la clave al cliente solicitante
            netOut.writeUTF("PRIVATE_KEY:" + encodedKey);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void closeResources() {
        try {
            if (netIn != null) netIn.close();
            if (netOut != null) netOut.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
