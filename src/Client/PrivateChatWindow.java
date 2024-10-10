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
    private SecretKey sharedKey; // Clave de cifrado compartida

    // Constructor de la ventana de chat privado
    public PrivateChatWindow(String sender, String recipient, Socket privateSocket, SecretKey sharedKey) {
        this.sender = sender;
        this.recipient = recipient;
        this.sharedKey = sharedKey;

        try {
            // Inicializar flujos de entrada y salida para el chat privado
            this.privateInput = new DataInputStream(privateSocket.getInputStream());
            this.privateOutput = new DataOutputStream(privateSocket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }

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
            System.out.println("Enviando mensaje privado: " + message + " al destinatario: " + recipient); // Depuración

            // Generar la clave secreta
            SecretKey secretKey = EncryptionChat.keyGenerator();

            // Encriptar el mensaje usando la clave generada
            String encryptedMessage = EncryptionChat.Encrypt(message, secretKey);

            // Codificar la clave secreta a String (Base64)
            String encodedKey = EncryptionChat.toString(secretKey);

            // Enviar el mensaje junto con la clave
            privateOutput.writeUTF("PRIVATE:" + recipient + ":" + encryptedMessage + ":" + encodedKey);
            
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
            // Crear un selector de archivos
            JFileChooser fileChooser = new JFileChooser();
            int result = fileChooser.showOpenDialog(this);
    
            if (result == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
    
                // Validar que el archivo no sea mayor a 50 MB
                if (selectedFile.length() > 50 * 1024 * 1024) { // 50 MB en bytes
                    JOptionPane.showMessageDialog(this, "El archivo es demasiado grande. Elija un archivo de menos de 50 MB.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
    
                // Enviar el archivo al destinatario
                sendFileToRecipient(selectedFile);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    // Enviar archivo al destinatario
    private void sendFileToRecipient(File file) {
        try {
            // Enviar la notificación de envío de archivo al servidor
            privateOutput.writeUTF("FILE:" + recipient + ":" + file.getName() + ":" + file.length());
    
            // Leer el archivo y enviarlo por el socket
            FileInputStream fileInputStream = new FileInputStream(file);
            byte[] buffer = new byte[4096];
            int bytesRead;
    
            while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                privateOutput.write(buffer, 0, bytesRead);
            }
    
            fileInputStream.close();
            privateChatArea.append("Archivo enviado: " + file.getName() + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Clase interna para escuchar mensajes privados
    private class PrivateMessageListener implements Runnable {
        public void run() {
            try {
                while (true) {
                    String msg = privateInput.readUTF();
                    System.out.println("Mensaje privado recibido: " + msg); // Depuración

                    if (msg.startsWith("FILE:" + sender)) {
                        String[] parts = msg.split(":", 4);
                        String fileName = parts[2];
                        long fileSize = Long.parseLong(parts[3]);

                        // Recibir el archivo y guardarlo en la carpeta de descargas
                        receiveFile(fileName, fileSize);
                    } else if (msg.startsWith("PRIVATE:" + sender)) {
                        // Lógica existente para recibir mensajes privados
                        String[] parts = msg.split(":", 4);
                        String sender = parts[1];
                        String encryptedMessage = parts[2];
                        String encodedKey = parts[3];

                        // Convertir la clave desde Base64
                        SecretKey secretKey = EncryptionChat.toSecretKey(encodedKey);

                        try {
                            // Desencriptar el mensaje usando la clave recibida
                            String decryptedMessage = EncryptionChat.Dencrypt(encryptedMessage, secretKey);
                            privateChatArea.append(sender + ": " + decryptedMessage + "\n");
                        } catch (Exception e) {
                            e.printStackTrace();
                            privateChatArea.append("Error al desencriptar el mensaje.\n");
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    
    // Recibir archivo
    private void receiveFile(String fileName, long fileSize) {
        try {
            // Obtener la ruta de la carpeta de descargas
            String userHome = System.getProperty("user.home");
            File downloadFolder = new File(userHome, "Downloads");
            File receivedFile = new File(downloadFolder, fileName);
    
            FileOutputStream fileOutputStream = new FileOutputStream(receivedFile);
            byte[] buffer = new byte[4096];
            int bytesRead;
            long totalBytesRead = 0;
    
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