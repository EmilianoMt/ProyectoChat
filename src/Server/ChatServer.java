package src.Server;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class ChatServer {
    private static final int GROUP_PORT = 8081;
    private static final int PRIVATE_PORT = 8082;
    private static Set<String> connectedUsers = ConcurrentHashMap.newKeySet(); // Thread-safe set for users
    private static Map<String, Socket> userSockets = new ConcurrentHashMap<>(); // Store users' sockets
    private static ExecutorService executor = Executors.newCachedThreadPool();

    public static void main(String[] args) {
        try {
            // Create two server sockets for group and private chat
            ServerSocket groupServerSocket = new ServerSocket(GROUP_PORT);
            ServerSocket privateServerSocket = new ServerSocket(PRIVATE_PORT);

            System.out.println("Chat Server is running on ports " + GROUP_PORT + " (group) and " + PRIVATE_PORT + " (private)");

            // Start a thread to handle group chat connections
            new Thread(() -> handleGroupConnections(groupServerSocket)).start();

            // Start a thread to handle private chat connections
            new Thread(() -> handlePrivateConnections(privateServerSocket)).start();
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Handle group chat connections
    private static void handleGroupConnections(ServerSocket serverSocket) {
        try {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                executor.submit(new HiloChatServer(clientSocket, "group"));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Handle private chat connections
    private static void handlePrivateConnections(ServerSocket serverSocket) {
        try {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                executor.submit(new HiloChatServer(clientSocket, "private"));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Add user and update user list
    public static synchronized void addUser(String username, Socket socket) {
        connectedUsers.add(username);
        userSockets.put(username, socket);
        sendUserListToAll();
    }

    // Remove user and update user list
    public static synchronized void removeUser(String username) {
        connectedUsers.remove(username);
        userSockets.remove(username);
        sendUserListToAll();
    }

    // Send updated user list to all clients
    public static synchronized void sendUserListToAll() {
        String userList = "USERS:" + String.join(",", connectedUsers);
        for (Socket socket : userSockets.values()) {
            try {
                DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                out.writeUTF(userList);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Send a private message to a specific user
    public static synchronized void sendPrivateMessage(String recipient, String message) {
        try {
            Socket recipientSocket = userSockets.get(recipient);
            if (recipientSocket != null) {
                DataOutputStream outPrivate = new DataOutputStream(recipientSocket.getOutputStream());
                outPrivate.writeUTF(message);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Broadcast a group message to all users
    public static synchronized void broadcastMessage(String message) {
        for (Socket s : userSockets.values()) {
            try {
                DataOutputStream out = new DataOutputStream(s.getOutputStream());
                out.writeUTF(message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}