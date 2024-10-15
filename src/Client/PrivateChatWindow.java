package src.Client;

import java.awt.*;
import java.io.*;
import javax.crypto.SecretKey;
import javax.swing.*;

public class PrivateChatWindow extends JFrame {
    private DataInputStream privateInput;
    private DataOutputStream privateOutput;
    private static JTextArea privateChatArea;
    private JTextField privateMessageField;
    private String sender;
    private String recipient;
    private SecretKey sharedKey; // Clave de cifrado compartida
    private ChatClient chatClient;  // Referencia al cliente de chat para eliminar la ventana

    // Constructor de la ventana de chat privado
    public PrivateChatWindow(String sender, String recipient, DataInputStream groupInput, DataOutputStream groupOutput, SecretKey sharedKey, ChatClient chatClient) {
        this.sender = sender;
        this.recipient = recipient;
        this.privateInput = groupInput; // Usar el mismo flujo de entrada
        this.privateOutput = groupOutput; // Usar el mismo flujo de salida
        this.sharedKey = sharedKey;
        this.chatClient = chatClient;

        // Inicializar la interfaz de usuario
        setupUI();

        // Iniciar la escucha de mensajes privados
        new Thread(new PrivateMessageListener()).start();
    }

    // Configuración de la interfaz de usuario
    private void setupUI() {
        setTitle("Private Chat - " + recipient);
        setSize(400, 300);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        // Área de chat privado
        privateChatArea = new JTextArea();
        privateChatArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(privateChatArea);
        add(scrollPane, BorderLayout.CENTER);

        // Panel para los botones
        JPanel bottomPanel = new JPanel(new BorderLayout());

        // Campo de mensaje
        privateMessageField = new JTextField();
        bottomPanel.add(privateMessageField, BorderLayout.CENTER);

        // Botón para enviar mensaje
        JButton sendButton = new JButton("Enviar");
        sendButton.addActionListener(e -> sendPrivateMessage());
        bottomPanel.add(sendButton, BorderLayout.EAST);

        // Botón para enviar archivo
        JButton sendFileButton = new JButton("Enviar archivo");
        sendFileButton.addActionListener(e -> sendFile());
        bottomPanel.add(sendFileButton, BorderLayout.WEST);

        add(bottomPanel, BorderLayout.SOUTH);
    }

    // Enviar mensaje privado
    private void sendPrivateMessage() {
        try {
            String message = privateMessageField.getText().trim();
            if (!message.isEmpty()) {
                if (sharedKey == null) {
                    System.err.println("No se puede encriptar el mensaje. La clave compartida es nula.");
                    JOptionPane.showMessageDialog(this, "No se puede enviar el mensaje. La clave compartida es nula.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                // Encriptar el mensaje usando la clave compartida
                String encryptedMessage = EncryptionChat.Encrypt(message, sharedKey);

                // Enviar el mensaje privado con el prefijo "PRIVATE:"
                privateOutput.writeUTF("PRIVATE:" + recipient + ":" + encryptedMessage);

                // Mostrar el mensaje en la ventana de chat (no encriptado)
                privateChatArea.append("Me: " + message + "\n");
                privateMessageField.setText("");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Enviar archivo
    private void sendFile() {
        try {
            JFileChooser fileChooser = new JFileChooser();
            int result = fileChooser.showOpenDialog(this);
    
            if (result == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
    
                // Validar que el archivo no sea mayor a 50 MB
                if (selectedFile.length() > 50 * 1024 * 1024) {
                    JOptionPane.showMessageDialog(this, "El archivo es demasiado grande. Elija un archivo de menos de 50 MB.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
    
                // Leer el archivo en bytes
                byte[] fileBytes = new byte[(int) selectedFile.length()];
                FileInputStream fileInputStream = new FileInputStream(selectedFile);
                fileInputStream.read(fileBytes);
                fileInputStream.close();
    
                // Enviar el archivo por bytes
                privateOutput.writeUTF("FILE:" + recipient + ":" + selectedFile.getName() + ":" + fileBytes.length);
                privateOutput.write(fileBytes); 
                privateOutput.flush();

                System.out.println("Archivo enviado: " + selectedFile.getName()); // Depuración
    
                privateChatArea.append("Archivo enviado: " + selectedFile.getName() + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Clase interna para escuchar mensajes privados (desde el mismo Socket)
    private class PrivateMessageListener implements Runnable {
        public void run() {
            try {
                while (true) {
                    String msg = privateInput.readUTF();  // Usar el mismo flujo de entrada (groupInput)
                    System.out.println("Mensaje privado recibido: " + msg); // Depuración

                    if (msg.startsWith("PRIVATE:" + sender)) {
                        reciveMessage(msg);
                    } else if (msg.startsWith("FILE:")) {
                        handleFileReception(msg);
                    } else {
                        System.out.println("Mensaje privado mal formado: " + msg);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Procesar mensaje privado para este remitente
    private void reciveMessage(String msg) {
        String[] parts = msg.split(":", 3);
        String encryptedMessage = parts[2];

        if (sharedKey == null) {
            System.err.println("No se puede desencriptar el mensaje. La clave compartida es nula.");
            privateChatArea.append("Error: Clave compartida no disponible para desencriptar el mensaje.\n");
            return;
        }

        // Desencriptar el mensaje usando la clave compartida
        try {
            String decryptedMessage = EncryptionChat.Dencrypt(encryptedMessage, sharedKey);
            privateChatArea.append(sender + ": " + decryptedMessage + "\n");
        } catch (Exception e) {
            e.printStackTrace();
            privateChatArea.append("Error al desencriptar el mensaje.\n");
        }
    }

    // Manejar la recepción de un archivo
    private void handleFileReception(String msg) {
        String[] parts = msg.split(":", 3);
        if (parts.length == 3) {
            String fileName = parts[1];
            long fileSize = Long.parseLong(parts[2]);

            try {
                // Ruta para guardar el archivo
                String userHome = System.getProperty("user.home");
                File downloadFolder = new File(userHome, "Downloads");
                File receivedFile = new File(downloadFolder, fileName);

                FileOutputStream fileOutputStream = new FileOutputStream(receivedFile);
                byte[] buffer = new byte[4096];
                int bytesRead;
                long totalBytesRead = 0;

                // Leer el archivo en bytes y guardarlo
                while (totalBytesRead < fileSize && (bytesRead = privateInput.read(buffer)) != -1) {
                    fileOutputStream.write(buffer, 0, bytesRead);
                    totalBytesRead += bytesRead;
                }

                fileOutputStream.close();
                privateChatArea.append("Archivo recibido: " + fileName + " guardado en " + downloadFolder.getAbsolutePath() + "\n");
            } catch (IOException e) {
                e.printStackTrace();
                privateChatArea.append("Error al recibir el archivo.\n");
            }
        }
    }
    
    @Override
    public void dispose() {
        // No cerrar los recursos del socket, solo cerrar la ventana
        System.out.println("Cerrando ventana de chat privado para: " + recipient);
        chatClient.removePrivateChatWindow(recipient); 
        super.dispose();  // Llamar al método de la superclase para cerrar la ventana
    }

    // Obtener el área de chat privado
    public static JTextArea getPrivateArea() {
        return privateChatArea;
    }
}
