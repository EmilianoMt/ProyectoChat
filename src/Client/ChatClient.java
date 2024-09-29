package src.Client;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
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

            username = JOptionPane.showInputDialog("Introduce tu nombre:");
            output.writeUTF(username); // Enviar el nombre de usuario al servidor

            initializeUI();
            startChat();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "No se pudo conectar al servidor.");
            e.printStackTrace();
            return;
        }
    }

    private void initializeUI() {
        setTitle("Chat Client - " + username);
        setSize(500, 400); // Aumentar el tamaño de la ventana
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        JScrollPane scrollPane = new JScrollPane(chatArea);

        messageField = new JTextField();
        sendButton = new JButton("Send");
        sendButton.addActionListener(this);

        // Lista de usuarios conectados
        userListModel = new DefaultListModel<>();
        userList = new JList<>(userListModel);
        JScrollPane userScrollPane = new JScrollPane(userList);
        userScrollPane.setPreferredSize(new Dimension(150, 0)); // Ajustar tamaño de la lista

        // Panel principal
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(messageField, BorderLayout.CENTER);
        panel.add(sendButton, BorderLayout.EAST);

        add(scrollPane, BorderLayout.CENTER);
        add(userScrollPane, BorderLayout.EAST); // Agregar la lista de usuarios a la derecha
        add(panel, BorderLayout.SOUTH);

        setVisible(true);
    }

    private void startChat() {
        new Thread(() -> {
            try {
                while (true) {
                    String message = input.readUTF();
                    if (message.startsWith("USERS:")) {
                        // Actualizar la lista de usuarios
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

    private void closeResources() {
        try {
            if (input != null) input.close();
            if (output != null) output.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new ChatClient("localhost", 8081).setVisible(true);
        });
    }
}

