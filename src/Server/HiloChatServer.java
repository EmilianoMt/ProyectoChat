package src.Server;

import java.io.*;
import java.net.*;
import java.util.Base64;
import javax.crypto.SecretKey;

public class HiloChatServer implements Runnable {
    private Socket socket;
    private DataInputStream input;
    private String username;
    private String key;
    SecretKey sharedKey;

    // Constructor para inicializar el socket y el tipo de chat
    public HiloChatServer(Socket socket) {
        this.socket = socket;
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
            System.out.println(username + " connected."); // Depuración
            ChatServer.addUser(username, socket);

            sharedKey = ChatServer.generateSharedKey();
            key = toString(sharedKey); 
            System.out.println(key);

            // Enviar la clave compartida a ambos usuarios
            ChatServer.sendSharedKeyToAllClients(sharedKey);
            System.out.println("Clave compartida enviada a todos los usuarios" + key);


            String msg;
            // Leer continuamente mensajes del cliente
            while ((msg = input.readUTF()) != null) {
                System.out.println("Recibido mensaje del cliente: " + msg); // Depuración

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
            ChatServer.broadcastMessage(username + ": " + msg); // Difundir mensaje a todos los usuarios
    }

    // Manejar mensajes privados
   private void handlePrivateMessage(String msg) {
        System.out.println("Recibiendo mensaje privado en el servidor: " + msg); // Depuración

        String[] parts = msg.split(":", 3);
        if (parts.length < 3) {
            System.out.println("Malformed private message: " + msg);
            return;
        }

        String recipient = parts[1]; // Extraer el nombre del destinatario
        String message = parts[2];

        // Generar clave compartida
        System.out.println("Generando y compartiendo la clave secreta para: " + username + " y " + recipient);


        

        // Enviar el mensaje encriptado con la clave compartida
        System.out.println("Enviando mensaje privado al destinatario: " + recipient + "   " + message); // Depuración
        ChatServer.sendPrivateMessage(recipient, "PRIVATE:" + username + ": " + message);
    }

     public static String toString(SecretKey secretKey) {
            return Base64.getEncoder().encodeToString(secretKey.getEncoded());
        }

    // Cerrar flujo de entrada y socket
    private void closeResources() {
        try {
            if (input != null) input.close();
            if (socket != null) socket.close();
            System.out.println("Recursos cerrados para el usuario: " + username);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}