package src.Server;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class ChatServer {
    private static final int PORT = 8081; // Puerto para el chat grupal

    private static Set<String> connectedUsers = ConcurrentHashMap.newKeySet(); // Conjunto de usuarios conectados (thread-safe)
    private static Map<String, Socket> userSockets = new ConcurrentHashMap<>(); // Mapa de sockets de usuarios
    private static ExecutorService executor = Executors.newCachedThreadPool(); // Pool de hilos para manejar conexiones

    public static void main(String[] args) {
        try {
            // Crear dos sockets de servidor para chat grupal y privado
            
            ServerSocket serverSocket = new ServerSocket(PORT);

            System.out.println("Chat Server is running on port " + PORT);

            while (true) {               
                Socket clientSocket = serverSocket.accept(); // Aceptar nueva conexión
                executor.submit(new HiloChatServer(clientSocket)); // Asignar a un hilo
            }
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // // Manejar conexiones de chat grupal
    // private static void handleGroupConnections(ServerSocket serverSocket) {
    //     try {
    //         while (true) {
    //             Socket clientSocket = serverSocket.accept(); // Aceptar nueva conexión
    //             executor.submit(new HiloChatServer(clientSocket, "group")); // Asignar a un hilo
    //         }
    //     } catch (IOException e) {
    //         e.printStackTrace();
    //     }
    // }

    // // Manejar conexiones de chat privado
    // private static void handlePrivateConnections(ServerSocket serverSocket) {
    //     try {
    //         while (true) {
    //             Socket clientSocket = serverSocket.accept(); // Aceptar nueva conexión
    //             executor.submit(new HiloChatServer(clientSocket, "private")); // Asignar a un hilo
    //         }
    //     } catch (IOException e) {
    //         e.printStackTrace();
    //     }
    // }

    // Añadir usuario y actualizar lista de usuarios
    public static synchronized void addUser(String username, Socket socket) {
        
        connectedUsers.add(username); // Añadir usuario al conjunto
        userSockets.put(username, socket); // Añadir socket del usuario al mapa
        sendUserListToAll(); // Enviar lista de usuarios actualizada a todos
        broadcastMessage(username + " has joined the chat."); // Notificar a todos los usuarios
    }

    // Eliminar usuario y actualizar lista de usuarios
    public static synchronized void removeUser(String username) {
        connectedUsers.remove(username); // Eliminar usuario del conjunto
        userSockets.remove(username); // Eliminar socket del usuario del mapa
        sendUserListToAll(); // Enviar lista de usuarios actualizada a todos
        broadcastMessage(username + " has left the chat."); // Notificar a todos los usuarios
    }

    // Enviar lista de usuarios actualizada a todos los clientes
    public static synchronized void sendUserListToAll() {
        String userList = "USERS:" + String.join(",", connectedUsers); // Crear cadena con lista de usuarios
        for(Map.Entry<String, Socket> entry : userSockets.entrySet()) {
            try {
                Socket socket = entry.getValue();
                if(socket != null && !socket.isClosed() && socket.isConnected()) {
                    DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                    out.writeUTF(userList); // Enviar lista de usuarios

                }else{
                    System.out.println("Socket cerrado para el usuario: " + entry.getKey());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            
        }
    }

    // Enviar un mensaje privado a un usuario específico
    public static synchronized void sendPrivateMessage(String recipient, String message) {
        try {
            Socket recipientSocket = userSockets.get(recipient); // Obtener socket del destinatario
            if (recipientSocket != null) {
                DataOutputStream outPrivate = new DataOutputStream(recipientSocket.getOutputStream());
                outPrivate.writeUTF(message); // Enviar mensaje privado
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Difundir un mensaje grupal a todos los usuarios
    public static synchronized void broadcastMessage(String message) {
        for (Socket s : userSockets.values()) {
            try {
                DataOutputStream out = new DataOutputStream(s.getOutputStream());
                out.writeUTF(message); // Enviar mensaje grupal
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}