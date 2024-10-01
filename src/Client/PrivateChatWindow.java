package src.Client;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.Socket;
import javax.crypto.SecretKey;
import javax.swing.*;

public class PrivateChatWindow extends JFrame implements ActionListener {
    private Socket socket;
    private DataInputStream input;
    private DataOutputStream output;
    private JTextArea chatArea;
    private JTextField messageField;
    private JButton sendButton, fileButton;
    private String sender;
    private String recipient;
    private SecretKey secretKey;

    // Constructor de la ventana de chat privado
    public PrivateChatWindow(String sender, String recipient, Socket socket) {
        this.sender = sender;
        this.recipient = recipient;
        this.socket = socket;

        try {
            input = new DataInputStream(socket.getInputStream());
            output = new DataOutputStream(socket.getOutputStream());
            try {
                secretKey = EncryptionChat.generateKey(); // Generar clave secreta para encriptación
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        initializeUI(); // Inicializar la interfaz de usuario
        startPrivateChat(); // Iniciar el chat privado
    }

    // Inicializar la interfaz de usuario
    private void initializeUI() {
        setTitle("Chat Privado con " + recipient);
        setSize(400, 300);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        JScrollPane scrollPane = new JScrollPane(chatArea);

        messageField = new JTextField();
        sendButton = new JButton("Enviar");
        sendButton.addActionListener(this);

        fileButton = new JButton("Enviar Archivo");
        fileButton.addActionListener(e -> sendFile());

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(messageField, BorderLayout.CENTER);
        panel.add(sendButton, BorderLayout.EAST);
        panel.add(fileButton, BorderLayout.WEST);

        add(scrollPane, BorderLayout.CENTER);
        add(panel, BorderLayout.SOUTH);

        setVisible(true);
    }

    // Iniciar el chat privado en un hilo separado
    private void startPrivateChat() {
        new Thread(() -> {
            try {
                while (true) {
                    String encryptedMessage = input.readUTF();
                    String decryptedMessage;
                    try {
                        decryptedMessage = EncryptionChat.decrypt(encryptedMessage, secretKey); // Desencriptar mensaje
                    } catch (Exception e) {
                        decryptedMessage = "Error decrypting message.";
                        e.printStackTrace();
                    }
                    chatArea.append(decryptedMessage + "\n");
                }
            } catch (IOException e) {
                closeResources(); // Cerrar recursos en caso de error
            }
        }).start();
    }

    // Manejar el evento de acción (enviar mensaje)
    @Override
    public void actionPerformed(ActionEvent e) {
        sendMessage();
    }

    // Enviar un mensaje
    private void sendMessage() {
        String message = messageField.getText().trim();
        if (!message.isEmpty()) {
            try {
                String encryptedMessage = EncryptionChat.encrypt(sender + ": " + message, secretKey); // Encriptar mensaje
                output.writeUTF("PRIVATE:" + recipient + ":" + encryptedMessage); // Especificar que es un mensaje privado
                messageField.setText("");
            } catch (IOException ex) {
                chatArea.append("Error enviando el mensaje.\n");
                ex.printStackTrace();
            } catch (Exception ex) {
                chatArea.append("Error encriptando el mensaje.\n");
                ex.printStackTrace();
            }
        }
    }

    // Enviar un archivo
    private void sendFile() {
        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showOpenDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            if (file.length() <= 50 * 1024 * 1024) {  // Limitar el tamaño del archivo a 50 MB
                try (FileInputStream fis = new FileInputStream(file)) {
                    output.writeUTF("FILE:" + file.getName() + ":" + file.length());
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = fis.read(buffer)) != -1) {
                        output.write(buffer, 0, bytesRead);
                    }
                    chatArea.append("Archivo enviado: " + file.getName() + "\n");
                } catch (IOException e) {
                    chatArea.append("Error enviando el archivo.\n");
                    e.printStackTrace();
                }
            } else {
                chatArea.append("El archivo supera el límite de 50 MB.\n");
            }
        }
    }

    // Cerrar recursos (streams y socket)
    private void closeResources() {
        try {
            if (input != null) input.close();
            if (output != null) output.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
