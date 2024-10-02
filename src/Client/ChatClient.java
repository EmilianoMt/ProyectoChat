package src.Client;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;
import javax.crypto.SecretKey;
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

    // Constructor que inicializa la conexión con el servidor y la interfaz de usuario
    public ChatClient(String serverAddress, int serverPort) {
        try {
            socket = new Socket(serverAddress, serverPort);
            input = new DataInputStream(socket.getInputStream());
            output = new DataOutputStream(socket.getOutputStream());

            // Solicita el nombre de usuario y lo envía al servidor
            username = JOptionPane.showInputDialog("Introduce tu nombre:");
            output.writeUTF(username);

            // Inicializa la interfaz de usuario
            initializeUI();
            // Inicia el chat
            startChat();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "No se pudo conectar al servidor.");
            e.printStackTrace();
            return;
        }
    }

    // Método para inicializar la interfaz de usuario
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

    // Método para abrir una ventana de chat privado
    private void openPrivateChat(String selectedUser) {
        // Solicitar clave compartida al servidor para encriptación privada
        try {
            // Enviar una solicitud al servidor para obtener una clave privada compartida con el usuario seleccionado
            output.writeUTF("REQUEST_PRIVATE_KEY:" + selectedUser);
            
            // Leer la respuesta del servidor que contiene la clave privada codificada
            String keyResponse = input.readUTF();
            
            // Declarar una variable para almacenar la clave secreta decodificada
            SecretKey secretKey;
            try {
                // Decodificar la clave privada recibida del servidor
                secretKey = EncryptionChat.decodeKey(keyResponse);
            } catch (Exception e) {
                // Mostrar un mensaje de error en el área de chat si hay un problema al decodificar la clave
                chatArea.append("Error decodificando la clave para " + selectedUser + ".\n");
                e.printStackTrace();
                // Salir del método si hay un error al decodificar la clave
                return;
            }
    
            // Crear una nueva ventana de chat privado con el usuario seleccionado y la clave secreta decodificada
            PrivateChatWindow privateChat = new PrivateChatWindow(username, selectedUser, socket, secretKey);
            // Hacer visible la ventana de chat privado
            privateChat.setVisible(true);
        } catch (IOException e) {
            // Mostrar un mensaje de error en el área de chat si hay un problema al abrir el chat privado
            chatArea.append("Error abriendo chat privado con " + selectedUser + ".\n");
            e.printStackTrace();
        }
    }

    // Método para iniciar el chat y manejar la recepción de mensajes
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

    // Método que se llama cuando se hace clic en el botón de enviar
    @Override
    public void actionPerformed(ActionEvent e) {
        sendMessage();
    }

    // Método para enviar un mensaje al servidor
    private void sendMessage() {
        String message = messageField.getText().trim();
        if (!message.isEmpty()) {
            try {
                output.writeUTF(username + ": " + message); 
                messageField.setText("");
            } catch (IOException ex) {
                chatArea.append("Error enviando el mensaje.\n");
                ex.printStackTrace();
            }
        }
    }

    // Método para cerrar los recursos de entrada, salida y el socket
    private void closeResources() {
        try {
            if (input != null) input.close();
            if (output != null) output.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Método principal para iniciar el cliente de chat
    public static void main(String[] args) {
        new ChatClient("localhost", 8081);
    }
}
