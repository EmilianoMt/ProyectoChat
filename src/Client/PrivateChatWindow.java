package src.Client;

import java.awt.*;
import java.io.*;
import java.net.*;
import javax.crypto.*;
import javax.swing.*;

public class PrivateChatWindow extends JFrame {
    private DataInputStream privateInput;
    private DataOutputStream privateOutput;
    private JTextArea privateChatArea;
    private JTextField privateMessageField;
    private String sender;
    private String recipient;
    private SecretKey sharedKey; // Shared encryption key

    public PrivateChatWindow(String sender, String recipient, Socket privateSocket, SecretKey sharedKey) {
        this.sender = sender;
        this.recipient = recipient;
        this.sharedKey = sharedKey;

        try {
            // Initialize input and output streams for private chat
            this.privateInput = new DataInputStream(privateSocket.getInputStream());
            this.privateOutput = new DataOutputStream(privateSocket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Initialize UI
        setupUI();

        // Start listening for private messages
        new Thread(new PrivateMessageListener()).start();
    }

    private void setupUI() {
        setTitle("Private Chat - " + recipient);
        setSize(400, 300);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        // Private chat area
        privateChatArea = new JTextArea();
        privateChatArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(privateChatArea);
        add(scrollPane, BorderLayout.CENTER);

        // Message field
        privateMessageField = new JTextField();
        privateMessageField.addActionListener(e -> sendPrivateMessage());
        add(privateMessageField, BorderLayout.SOUTH);
    }

    private void sendPrivateMessage() {
        try {
            String message = privateMessageField.getText().trim();
            if (!message.isEmpty()) {
                // Encrypt message using the shared key
                String encryptedMessage = EncryptionChat.encrypt(message, sharedKey);
                privateOutput.writeUTF("PRIVATE:" + recipient + ":" + encryptedMessage);
                privateChatArea.append("Me: " + message + "\n");
                privateMessageField.setText("");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class PrivateMessageListener implements Runnable {
        public void run() {
            try {
                while (true) {
                    String msg = privateInput.readUTF();
                    if (msg.startsWith("PRIVATE:" + sender)) {
                        String encryptedMessage = msg.split(":", 3)[2];
                        // Decrypt message using the shared key
                        String decryptedMessage = EncryptionChat.decrypt(encryptedMessage, sharedKey);
                        privateChatArea.append(recipient + ": " + decryptedMessage + "\n");
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}