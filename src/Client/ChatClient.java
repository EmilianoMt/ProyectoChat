package src.Client;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import javax.crypto.SecretKey;
import javax.swing.*;

public class ChatClient extends JFrame {
    private Socket socket;
    private Socket privateSocket;
    private DataInputStream groupInput;
    private DataOutputStream groupOutput;
    private DefaultListModel<String> userListModel;
    private JTextArea chatArea;
    private JTextField messageField;
    private JButton sendButton;
    private String username;
    private SecretKey sharedKey; // Clave de cifrado compartida

    // Constructor del cliente de chat
    public ChatClient(String serverAddress, int port) {
        try {
            // Seguir solicitando un nombre de usuario hasta que se ingrese uno válido
            while (username == null || username.trim().isEmpty()) {
                username = JOptionPane.showInputDialog(this, "Enter your username:");
                if (username == null || username.trim().isEmpty()) {
                    JOptionPane.showMessageDialog(this, "You must enter a username to join the chat!", "Warning", JOptionPane.WARNING_MESSAGE);
                }
            }

            // Conectar al servidor para el chat grupal
            socket = new Socket(serverAddress, port);
            groupInput = new DataInputStream(socket.getInputStream());
            groupOutput = new DataOutputStream(socket.getOutputStream());
            groupOutput.writeUTF(username);  // Enviar nombre de usuario al servidor

            // Conectar al servidor para el chat privado
            privateSocket = new Socket(serverAddress, port);

            // Generar una clave de cifrado compartida para los chats privados
            sharedKey = EncryptionChat.keyGenerator();

            // Inicializar la interfaz de usuario
            setupUI();

            // Comenzar a escuchar mensajes grupales
            new Thread(new GroupMessageListener()).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Configurar la interfaz de usuario
    private void setupUI() {
        setTitle(username + " - Group Chat");
        setSize(500, 400);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        // Área de chat
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);  // Ajustar texto a la siguiente línea si es demasiado largo
        chatArea.setWrapStyleWord(true);  // Ajustar en los límites de las palabras
        JScrollPane scrollPane = new JScrollPane(chatArea);
        add(scrollPane, BorderLayout.CENTER);

        // Campo de mensaje
        messageField = new JTextField(40);  // Campo de texto con más longitud
        messageField.addActionListener(e -> sendMessage());

        // Botón de enviar
        sendButton = new JButton("Send");
        sendButton.addActionListener(e -> sendMessage());

        // Panel para la entrada de mensajes
        JPanel messagePanel = new JPanel();
        messagePanel.setLayout(new BorderLayout());
        messagePanel.add(messageField, BorderLayout.CENTER);
        messagePanel.add(sendButton, BorderLayout.EAST);
        add(messagePanel, BorderLayout.SOUTH);

        // Lista de usuarios
        userListModel = new DefaultListModel<>();
        JList<String> userList = new JList<>(userListModel);
        userList.setFixedCellWidth(150);  // Aumentar el ancho de la lista de usuarios
        userList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    String selectedUser = userList.getSelectedValue();
                    if (selectedUser != null && !selectedUser.equals(username)) {
                        // Abrir una nueva ventana de chat privado con la clave de cifrado compartida
                        new PrivateChatWindow(username, selectedUser, privateSocket, sharedKey).setVisible(true);
                    }
                }
            }
        });
        JScrollPane userScrollPane = new JScrollPane(userList);
        add(userScrollPane, BorderLayout.EAST);
    }

    // Enviar mensaje al chat grupal
    private void sendMessage() {
        try {
            String message = messageField.getText().trim();
            if (!message.isEmpty()) {
                System.out.println("Enviando mensaje grupal: " + message); // Depuración
                groupOutput.writeUTF(message);  // Enviar solo el mensaje (sin concatenación de nombre de usuario)
                messageField.setText("");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    // Clase interna para escuchar mensajes grupales
    private class GroupMessageListener implements Runnable {
        public void run() {
            try {
                while (true) {
                    String msg = groupInput.readUTF();
                    System.out.println("Mensaje recibido: " + msg); // Depuración
                    if (msg.startsWith("USERS:")) {
                        // Actualizar la lista de usuarios solo si es un mensaje de lista de usuarios adecuado
                        String[] users = msg.substring(6).split(",");
                        updateUserList(users);
                        if (!msg.contains("PRIVATE:")) {
                            updateUserList(users);
                        }
                    } else if (!msg.startsWith("PRIVATE:")) {
                        // Mostrar mensaje grupal con un salto de línea (filtrar mensajes privados)
                        chatArea.append(msg + "\n");
                        chatArea.setCaretPosition(chatArea.getDocument().getLength());  // Desplazarse automáticamente al final
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Actualizar la lista de usuarios
    private void updateUserList(String[] users) {
        SwingUtilities.invokeLater(() -> {
            userListModel.clear();
            for (String user : users) {
                userListModel.addElement(user);
            }
        });
    }

    // Método principal para iniciar el cliente de chat
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ChatClient("localhost", 8081).setVisible(true));
    }
}