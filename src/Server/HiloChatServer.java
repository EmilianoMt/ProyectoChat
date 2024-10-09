package src.Server;

import java.io.*;
import java.net.*;

public class HiloChatServer implements Runnable {
    private Socket socket;
    private DataInputStream input;
    private String username;
    private String chatType; // Puede ser "group" o "private"

    // Constructor para inicializar el socket y el tipo de chat
    public HiloChatServer(Socket socket, String chatType) {
        this.socket = socket;
        this.chatType = chatType;
        try {
            this.input = new DataInputStream(socket.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            // Leer el nombre de usuario cuando el cliente se conecta
            username = input.readUTF();
            ChatServer.addUser(username, socket);

            String msg;
            // Leer continuamente mensajes del cliente
            while ((msg = input.readUTF()) != null) {
                if (msg.startsWith("PRIVATE:")) {
                    handlePrivateMessage(msg); // Manejar mensajes privados
                } else {
                    handleGroupMessage(msg); // Manejar mensajes grupales
                }
            }
        } catch (IOException e) {
            System.out.println(username + " disconnected.");
        } finally {
            ChatServer.removeUser(username); // Eliminar usuario al desconectarse
            closeResources(); // Cerrar recursos
        }
    }

    // Manejar mensajes grupales
    private void handleGroupMessage(String msg) {
        if (chatType.equals("group")) {
            ChatServer.broadcastMessage(username + ": " + msg); // Difundir mensaje a todos los usuarios
        }
    }

    // Manejar mensajes privados
    private void handlePrivateMessage(String msg) {
        if (chatType.equals("private")) {
            String[] parts = msg.split(":", 3);
            if (parts.length < 3) {
                System.out.println("Malformed private message: " + msg);
                return;
            }

            String recipient = parts[1]; // Extraer el nombre del destinatario
            String message = parts[2];
            ChatServer.sendPrivateMessage(recipient, "PRIVATE:" + username + ": " + message); // Enviar mensaje privado
        }
    }

    // Cerrar flujo de entrada y socket
    private void closeResources() {
        try {
            if (input != null) input.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}