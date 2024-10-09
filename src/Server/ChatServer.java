package src.Server;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class ChatServer {
    private static final int GROUP_PORT = 8081; // Puerto para el chat grupal
    private static final int PRIVATE_PORT = 8082; // Puerto para el chat privado
    private static Set<String> connectedUsers = ConcurrentHashMap.newKeySet(); // Conjunto de usuarios conectados (thread-safe)
    private static Map<String, Socket> userSockets = new ConcurrentHashMap<>(); // Mapa de sockets de usuarios
    private static ExecutorService executor = Executors.newCachedThreadPool(); // Pool de hilos para manejar conexiones

    public static void main(String[] args) {
        try {
            // Crear dos sockets de servidor para chat grupal y privado
            ServerSocket groupServerSocket = new ServerSocket(GROUP_PORT);
            ServerSocket privateServerSocket = new ServerSocket(PRIVATE_PORT);

            System.out.println("Chat Server is running on ports " + GROUP_PORT + " (group) and " + PRIVATE_PORT + " (private)");

            // Iniciar un hilo para manejar conexiones de chat grupal
            new Thread(() -> handleGroupConnections(groupServerSocket)).start();

            // Iniciar un hilo para manejar conexiones de chat privado
            new Thread(() -> handlePrivateConnections(privateServerSocket)).start();
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Manejar conexiones de chat grupal
    private static void handleGroupConnections(ServerSocket serverSocket) {
        try {
            while (true) {
                Socket clientSocket = serverSocket.accept(); // Aceptar nueva conexión
                executor.submit(new HiloChatServer(clientSocket, "group")); // Asignar a un hilo
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Manejar conexiones de chat privado
    private static void handlePrivateConnections(ServerSocket serverSocket) {
        try {
            while (true) {
                Socket clientSocket = serverSocket.accept(); // Aceptar nueva conexión
                executor.submit(new HiloChatServer(clientSocket, "private")); // Asignar a un hilo
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Añadir usuario y actualizar lista de usuarios
    public static synchronized void addUser(String username, Socket socket) {
        connectedUsers.add(username); // Añadir usuario al conjunto
        userSockets.put(username, socket); // Añadir socket del usuario al mapa
        sendUserListToAll(); // Enviar lista de usuarios actualizada a todos
    }

    // Eliminar usuario y actualizar lista de usuarios
    public static synchronized void removeUser(String username) {
        connectedUsers.remove(username); // Eliminar usuario del conjunto
        userSockets.remove(username); // Eliminar socket del usuario del mapa
        sendUserListToAll(); // Enviar lista de usuarios actualizada a todos
    }

    // Enviar lista de usuarios actualizada a todos los clientes
    public static synchronized void sendUserListToAll() {
        String userList = "USERS:" + String.join(",", connectedUsers); // Crear cadena con lista de usuarios
        for (Socket socket : userSockets.values()) {
            try {
                DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                out.writeUTF(userList); // Enviar lista de usuarios
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