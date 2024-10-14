package src.Client;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.HashMap;
import javax.crypto.SecretKey;
import javax.swing.*;

public class ChatClient extends JFrame {
    private Socket socket;
    private DataInputStream groupInput;
    private DataOutputStream groupOutput;
    private DefaultListModel<String> userListModel;
    private JTextArea chatArea;
    private JTextField messageField;
    private JButton sendButton;
    private String username;
    // private SecretKey sharedKey; // Clave de cifrado compartida 
    private HashMap<String, SecretKey> privateKeys = new HashMap<>();  // Almacenar claves privadas por usuario

    // Mapa para almacenar ventanas de chat privado abiertas
    private HashMap<String, PrivateChatWindow> privateChatWindows = new HashMap<>();

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

            // Conectar al servidor
            socket = new Socket(serverAddress, port);
            groupInput = new DataInputStream(socket.getInputStream());
            groupOutput = new DataOutputStream(socket.getOutputStream());
            groupOutput.writeUTF(username);  // Enviar nombre de usuario al servidor

            // Inicializar la interfaz de usuario
            setupUI();

            // Comenzar a escuchar mensajes (grupales, privados y claves compartidas)
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
                        openPrivateChatWindow(selectedUser);
                    }
                }
            }
        });
        JScrollPane userScrollPane = new JScrollPane(userList);
        add(userScrollPane, BorderLayout.EAST);
    }

    // Método para abrir una ventana de chat privado
    private void openPrivateChatWindow(String recipient) {
        if (!privateChatWindows.containsKey(recipient)) {
            // Verificar si la clave compartida para este usuario ya está disponible
            SecretKey keyForUser = privateKeys.get(recipient);
            if (keyForUser == null) {
                // Mostrar un mensaje si la clave aún no ha sido recibida
                JOptionPane.showMessageDialog(this, "La clave compartida aún no ha sido recibida para " + recipient, "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                // Crear una nueva ventana de chat privado sin nuevos sockets, usando los ya existentes
                PrivateChatWindow privateChat = new PrivateChatWindow(username, recipient, groupInput, groupOutput, keyForUser, this);
                privateChat.setVisible(true);
                privateChatWindows.put(recipient, privateChat);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            // Si la ventana ya está abierta, traerla al frente
            PrivateChatWindow privateChat = privateChatWindows.get(recipient);
            privateChat.toFront();
        }
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

    // Clase interna para escuchar mensajes (grupales, privados y claves compartidas)
    private class GroupMessageListener implements Runnable {
        public void run() {
            try {
                while (true) {
                    String msg = groupInput.readUTF();  // Escuchar el mismo Socket
                    System.out.println("Mensaje recibido: " + msg); // Depuración

                    if (msg.startsWith("USERS:")) {
                        // Actualizar la lista de usuarios
                        String[] users = msg.substring(6).split(",");
                        updateUserList(users);
                    } else if (msg.startsWith("KEY:")) {
                        // Recibir la clave compartida y asociarla con el usuario correcto
                        handleKeyMessage(msg);
                    } else if (msg.startsWith("PRIVATE:")) {
                        // Manejar mensaje privado
                        handlePrivateMessage(msg);
                    } else {
                        // Mostrar mensaje grupal
                        chatArea.append(msg + "\n");
                        chatArea.setCaretPosition(chatArea.getDocument().getLength());  // Desplazarse automáticamente al final
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    private void handlePrivateMessage(String msg) {
        String[] parts = msg.split(":", 3); // Dividir el mensaje en partes
        if (parts.length < 3) {
            System.out.println("Mensaje privado mal formado: " + msg);
            return;
        }
    
        String sender = parts[1];
        String encryptedMessage = parts[2].trim();
    
        // Si el nombre del remitente tiene "PRIVATE", quitarlo
        if (sender.startsWith("PRIVATE")) {
            sender = sender.replace("PRIVATE", "").trim();
        }
    
        // Obtener la clave privada del remitente
        SecretKey keyForUser = privateKeys.get(sender);
        if (keyForUser == null) {
            System.out.println("No se encontró la clave para " + sender);
            return;
        }
    
        // Desencriptar el mensaje usando la clave privada
        try {
            String decryptedMessage = EncryptionChat.Dencrypt(encryptedMessage, keyForUser);
            System.out.println("Mensaje privado de " + sender + ": " + decryptedMessage);
    
            // Actualizar la ventana de chat privado
            openPrivateChatWindow(sender);
            PrivateChatWindow privateChat = privateChatWindows.get(sender);
            if (privateChat != null) {
                privateChat.getPrivateArea().append(sender + ": " + decryptedMessage + "\n");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    

    // Manejar la recepción de la clave compartida
    private void handleKeyMessage(String msg) {
        String[] parts = msg.split(":", 2);
        if (parts.length < 2) {
            System.out.println("Clave mal formada: " + msg);
            return;
        }

        String encodedKey = parts[1];
        SecretKey receivedKey = EncryptionChat.toSecretKey(encodedKey);

        // Asignar la clave recibida al propio cliente
        privateKeys.put(username, receivedKey);
        System.out.println("Clave compartida recibida para " + username + ": " + encodedKey);

        // Asignar la clave compartida a todos los usuarios conectados
        for (int i = 0; i < userListModel.getSize(); i++) {
            String user = userListModel.getElementAt(i);
            privateKeys.put(user, receivedKey);
            System.out.println("Clave compartida asignada a: " + user);
        }
    }


    // Actualizar la lista de usuarios
    private void updateUserList(String[] users) {
        SwingUtilities.invokeLater(() -> {
            userListModel.clear();
            for (String user : users) {
                if (!user.contains("PRIVATE")) {
                    userListModel.addElement(user);
                }
            }
        });
    }

    @Override
    public void dispose() {
        try {
            if (groupInput != null) groupInput.close();
            if (groupOutput != null) groupOutput.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        super.dispose();
    }

    public void removePrivateChatWindow(String recipient) {
        privateChatWindows.remove(recipient);
    }

    // Método principal para iniciar el cliente de chat
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ChatClient("localhost", 8081).setVisible(true));
    }
}
