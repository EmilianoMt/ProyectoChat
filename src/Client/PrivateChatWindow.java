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
    }

    // Configuración de la interfaz de usuario
    private void setupUI() {
        setTitle("Private Chat - con " + recipient);
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
