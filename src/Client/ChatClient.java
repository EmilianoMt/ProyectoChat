package src.Client;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import javax.crypto.SecretKey;
import javax.swing.*;

public class ChatClient extends JFrame {
    private Socket groupSocket;
    private Socket privateSocket;
    private DataInputStream groupInput;
    private DataOutputStream groupOutput;
    private DefaultListModel<String> userListModel;
    private JTextArea chatArea;
    private JTextField messageField;
    private JButton sendButton;
    private String username;
    private SecretKey sharedKey; // Shared encryption key

    public ChatClient(String serverAddress, int groupPort, int privatePort) {
        try {
            // Keep prompting for a username until a valid one is entered
            while (username == null || username.trim().isEmpty()) {
                username = JOptionPane.showInputDialog(this, "Enter your username:");
                if (username == null || username.trim().isEmpty()) {
                    JOptionPane.showMessageDialog(this, "You must enter a username to join the chat!", "Warning", JOptionPane.WARNING_MESSAGE);
                }
            }

            // Connect to server for group chat
            groupSocket = new Socket(serverAddress, groupPort);
            groupInput = new DataInputStream(groupSocket.getInputStream());
            groupOutput = new DataOutputStream(groupSocket.getOutputStream());
            groupOutput.writeUTF(username);  // Send username to the server

            // Connect to server for private chat
            privateSocket = new Socket(serverAddress, privatePort);

            // Generate a shared encryption key for private chats
            sharedKey = EncryptionChat.generateKey();

            // Initialize UI
            setupUI();

            // Start listening for group messages
            new Thread(new GroupMessageListener()).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setupUI() {
        setTitle(username + " - Group Chat");
        setSize(700, 500);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        // Chat area
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);  // Wrap text to next line if too long
        chatArea.setWrapStyleWord(true);  // Wrap on word boundaries
        JScrollPane scrollPane = new JScrollPane(chatArea);
        add(scrollPane, BorderLayout.CENTER);

        // Message field
        messageField = new JTextField(40);  // Text field with more length
        messageField.addActionListener(e -> sendMessage());

        // Send button
        sendButton = new JButton("Send");
        sendButton.addActionListener(e -> sendMessage());

        // Panel for message input
        JPanel messagePanel = new JPanel();
        messagePanel.setLayout(new BorderLayout());
        messagePanel.add(messageField, BorderLayout.CENTER);
        messagePanel.add(sendButton, BorderLayout.EAST);
        add(messagePanel, BorderLayout.SOUTH);

        // User list
        userListModel = new DefaultListModel<>();
        JList<String> userList = new JList<>(userListModel);
        userList.setFixedCellWidth(150);  // Increase width of the user list
        userList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    String selectedUser = userList.getSelectedValue();
                    if (selectedUser != null && !selectedUser.equals(username)) {
                        // Open a new private chat window with the shared encryption key
                        new PrivateChatWindow(username, selectedUser, privateSocket, sharedKey).setVisible(true);
                    }
                }
            }
        });
        JScrollPane userScrollPane = new JScrollPane(userList);
        add(userScrollPane, BorderLayout.EAST);
    }

    private void sendMessage() {
        try {
            String message = messageField.getText().trim();
            if (!message.isEmpty()) {
                groupOutput.writeUTF(message);  // Send only the message (no username concatenation)
                messageField.setText("");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class GroupMessageListener implements Runnable {
        public void run() {
            try {
                while (true) {
                    String msg = groupInput.readUTF();
                    if (msg.startsWith("USERS:")) {
                        // Update user list only if it is a proper user list message
                        String[] users = msg.substring(6).split(",");
                        if (!msg.contains("PRIVATE:")) {
                            updateUserList(users);
                        }
                    } else if (!msg.startsWith("PRIVATE:")) {
                        // Display group message with a newline (filter out private messages)
                        chatArea.append(msg + "\n");
                        chatArea.setCaretPosition(chatArea.getDocument().getLength());  // Auto-scroll to bottom
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void updateUserList(String[] users) {
        SwingUtilities.invokeLater(() -> {
            userListModel.clear();
            for (String user : users) {
                if (!user.contains("PRIVATE:")) {  // Filter out any messages with "PRIVATE:"
                    userListModel.addElement(user);
                }
            }
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ChatClient("localhost", 8081, 8082).setVisible(true));
    }
}