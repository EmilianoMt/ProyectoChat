package src.Server;

import java.io.*;
import java.net.*;

public class HiloChatServer implements Runnable {
    private Socket socket;
    private DataInputStream input;
    private String username;
    private String chatType; // Could be "group" or "private"

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
            username = input.readUTF(); // Get the username when client connects
            ChatServer.addUser(username, socket);

            String msg;
            while ((msg = input.readUTF()) != null) {
                if (msg.startsWith("PRIVATE:")) {
                    handlePrivateMessage(msg);
                } else {
                    handleGroupMessage(msg);
                }
            }
        } catch (IOException e) {
            System.out.println(username + " disconnected.");
        } finally {
            ChatServer.removeUser(username);
            closeResources();
        }
    }

    // Handle group messages
    private void handleGroupMessage(String msg) {
        if (chatType.equals("group")) {
            ChatServer.broadcastMessage(username + ": " + msg);
        }
    }

    // Handle private messages
    private void handlePrivateMessage(String msg) {
        if (chatType.equals("private")) {
            String[] parts = msg.split(":", 3);
            if (parts.length < 3) {
                System.out.println("Malformed private message: " + msg);
                return;
            }

            String recipient = parts[1]; // Extract the recipient's name
            String message = parts[2];
            ChatServer.sendPrivateMessage(recipient, "PRIVATE:" + username + ": " + message);
        }
    }

    private void closeResources() {
        try {
            if (input != null) input.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}