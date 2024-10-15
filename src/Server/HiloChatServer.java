package src.Server;

import java.io.*;
import java.net.*;
import java.util.Base64;
import javax.crypto.SecretKey;

public class HiloChatServer implements Runnable {
    private Socket socket;
    private DataInputStream input;
    private String username;
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
            System.out.println(username + " connected.");
            
            // Añadir al usuario en el servidor
            ChatServer.addUser(username, socket);

            // Enviar la clave compartida al nuevo cliente
            sharedKey = ChatServer.getSharedKey(); // Obtener la clave generada en el servidor
            System.out.println("Clave enviada al cliente: " + sharedKey);

            // Enviar la clave al nuevo cliente
            ChatServer.sendSharedKeyToClient(sharedKey, socket);

            // Continuar recibiendo mensajes del cliente
            String msg;
            while ((msg = input.readUTF()) != null) {
                if (msg.startsWith("PRIVATE:")) {
                    handlePrivateMessage(msg); // Manejar mensajes privados
                }else if (msg.startsWith("FILE:")) {
                    handleFileReception(msg); // Manejar archivos
                } else {
                    handleGroupMessage(msg); // Manejar mensajes grupales
                }
            }
        } catch (IOException e) {
            System.out.println(username + " disconnected.");
        } finally {
            ChatServer.removeUser(username);
            closeResources();
        }
    }



    // Manejar mensajes grupales
    private void handleGroupMessage(String msg) {
            ChatServer.broadcastMessage(username + ": " + msg); // Difundir mensaje a todos los usuarios
    }

    // Manejar mensajes privados
    private void handlePrivateMessage(String msg) {
        System.out.println("Recibiendo mensaje privado en el servidor: " + msg); // Depuración
            String[] parts = msg.split(":", 3); // dividir mensaje en partes
            if (parts.length < 3) {
                System.out.println("Mensaje privado mal formado: " + msg);
                return;
            }
        
            // Extraer destinatario y mensaje encriptado
            String recipient = parts[1];
            String message = parts[2];
        
            // Verificar que el destinatario y el mensaje no estén vacíos o corruptos
            if (recipient == null || recipient.isEmpty()) {
                System.out.println("El destinatario del mensaje privado es nulo o vacío.");
                return;
            }
        
            if (message == null || message.isEmpty()) {
                System.out.println("El mensaje privado está vacío o es nulo.");
                return;
            }
        
            // Enviar mensaje al destinatario corregido
            System.out.println("Enviando mensaje privado al destinatario: " + recipient);
            ChatServer.sendPrivateMessage(recipient, "PRIVATE:" + this.username + ":" + message);
        }

    private void handleFileReception(String msg){
         // Procesar archivo
         System.out.println("Recibiendo archivo en el servidor: " + msg);
         String[] parts = msg.split(":", 4);
         if (parts.length == 4) {
             String recipient = parts[1];
             String fileName = parts[2];
             long fileSize = Long.parseLong(parts[3]);
            //  System.out.println(fileSize);
            //  System.out.println(recipient);
 
             try {
                 // Leer el archivo del flujo de entrada
                 byte[] fileBytes = new byte[(int) fileSize];
                 input.readFully(fileBytes);  // Leer el archivo completo

                 ChatServer.sendFileToUser(recipient, fileName, fileBytes);
             } catch (IOException e) {
                 e.printStackTrace();
             }
         }
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