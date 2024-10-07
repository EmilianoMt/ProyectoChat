package src.Client;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;
import java.util.Base64;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.*;

public class ChatClient extends JFrame implements ActionListener {
    private Socket socket;
    private DataInputStream input;
    private DataOutputStream output;
    private JTextArea chatArea;
    private JTextField messageField;
    private JButton sendButton;
    private String username;
    private DefaultListModel<String> userListModel;
    private JList<String> userList;

    public ChatClient(String serverAddress, int serverPort) {
        try {
            socket = new Socket(serverAddress, serverPort);
            input = new DataInputStream(socket.getInputStream());
            output = new DataOutputStream(socket.getOutputStream());

            do {
                username = JOptionPane.showInputDialog("Introduce tu nombre:");
                if (username == null || username.trim().isEmpty()) {
                    JOptionPane.showMessageDialog(this, "El nombre no puede estar vacío. Por favor, ingresa un nombre.");
                }
            } while (username == null || username.trim().isEmpty());

            try {
                output.writeUTF(username);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Error enviando el nombre al servidor.");
                e.printStackTrace();
                return;
            }

            initializeUI();
            startChat();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "No se pudo conectar al servidor.");
            e.printStackTrace();
            return;
        }
    }

    private void initializeUI() {
        setTitle("Chat Grupal - " + username);
        setSize(600, 400);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        JScrollPane scrollPane = new JScrollPane(chatArea);

        messageField = new JTextField();
        sendButton = new JButton("Enviar");
        sendButton.addActionListener(this);

        userListModel = new DefaultListModel<>();
        userList = new JList<>(userListModel);
        userList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    String selectedUser = userList.getSelectedValue();
                    if (selectedUser != null && !selectedUser.equals(username)) {
                        openPrivateChat(selectedUser);
                    }
                }
            }
        });
        JScrollPane userScrollPane = new JScrollPane(userList);
        userScrollPane.setPreferredSize(new Dimension(150, 0));

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(messageField, BorderLayout.CENTER);
        panel.add(sendButton, BorderLayout.EAST);

        add(scrollPane, BorderLayout.CENTER);
        add(userScrollPane, BorderLayout.EAST);
        add(panel, BorderLayout.SOUTH);

        setVisible(true);
    }

    private void openPrivateChat(String selectedUser) {
        try {
            Socket privateSocket = new Socket(socket.getInetAddress(), socket.getPort());

            DataOutputStream privateOutput = new DataOutputStream(privateSocket.getOutputStream());
            DataInputStream privateInput = new DataInputStream(privateSocket.getInputStream());

            privateOutput.writeUTF("REQUEST_PRIVATE_KEY:" + selectedUser);
            // output.writeUTF("REQUEST_PRIVATE_KEY:" + selectedUser);

            String keyResponse = privateInput.readUTF();
            // String keyResponse = input.readUTF();

            System.out.println("Clave secreta codificada: " + keyResponse);
            
            SecretKey secretKey = decodeKey(keyResponse);
            System.out.println("Clave secreta decodificada: " + secretKey);
            if (secretKey == null) {
                System.out.println("Error decodificando la clave privada.\n");
                return;
            }

            PrivateChatWindow privateChat = new PrivateChatWindow(username, selectedUser, privateSocket, secretKey);
            // PrivateChatWindow privateChat = new PrivateChatWindow(username, selectedUser, socket, secretKey);
            privateChat.setVisible(true);
        } catch (IOException e) {
            chatArea.append("Error abriendo chat privado con " + selectedUser + ".\n");
            e.printStackTrace();
        }
    }

    public static SecretKey decodeKey(String encodedKey) {
        try {
            if (encodedKey.startsWith("PRIVATE_KEY:")) {
                encodedKey = encodedKey.substring("PRIVATE_KEY:".length());
            }
            System.out.println("Clave Base64 recibida: '" + encodedKey + "'");
            byte[] decodedKey = Base64.getDecoder().decode(encodedKey);
           
            if (decodedKey.length != 16 && decodedKey.length != 24 && decodedKey.length != 32) {
                System.out.println("Error: Longitud de clave AES no válida. Longitud: " + decodedKey.length);
                return null;
            }
            
            return new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");
        } catch (IllegalArgumentException e) {
            System.out.println("Error: Clave Base64 no válida.");
            e.printStackTrace();
            return null;
        }
    }
    
    private void startChat() {
        new Thread(() -> {
            try {
                while (true) {
                    String message = input.readUTF();
                    if (message.startsWith("USERS:")) {
                        String[] users = message.substring(6).split(",");
                        userListModel.clear();
                        for (String user : users) {
                            userListModel.addElement(user);
                        }
                    } else {
                        chatArea.append(message + "\n");
                    }
                }
            } catch (IOException e) {
                closeResources();
            }
        }).start();

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                try {
                    output.writeUTF(username + " se ha desconectado.");
                    closeResources();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        messageField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    sendMessage();
                }
            }
        });
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        sendMessage();
    }
    
    private void sendMessage() {
        try {
            String msg = messageField.getText();
            output.writeUTF(username + ": " + msg);
            messageField.setText("");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void closeResources() {
        try {
            if (input != null) input.close();
            if (output != null) output.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        new ChatClient("localhost", 8081);
    }
}
