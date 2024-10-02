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
    public PrivateChatWindow(String sender, String recipient, Socket socket, SecretKey secretKey) {
        this.sender = sender;
        this.recipient = recipient;
        this.socket = socket;
        this.secretKey = secretKey; // Almacena la clave secreta
    
        try {
            input = new DataInputStream(socket.getInputStream());
            output = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    
        initializeUI(); // Inicializa la interfaz
        startPrivateChat(); // Inicia el chat privado
    }

    // Inicializar la interfaz de usuario
    private void initializeUI() {
        setTitle("Chat Privado con " + recipient); // Establece el título de la ventana
        setSize(400, 300); // Establece el tamaño de la ventana
        setDefaultCloseOperation(DISPOSE_ON_CLOSE); // Define la acción de cierre

        chatArea = new JTextArea();
        chatArea.setEditable(false); // Hace que el área de chat no sea editable
        chatArea.setLineWrap(true); // Habilita el ajuste de línea
        JScrollPane scrollPane = new JScrollPane(chatArea); // Añade un scroll al área de chat

        messageField = new JTextField();
        sendButton = new JButton("Enviar");
        sendButton.addActionListener(this); // Añade el listener para el botón de enviar

        fileButton = new JButton("Enviar Archivo");
        fileButton.addActionListener(e -> sendFile()); // Añade el listener para el botón de enviar archivo

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(messageField, BorderLayout.CENTER);
        panel.add(sendButton, BorderLayout.EAST);
        panel.add(fileButton, BorderLayout.WEST);

        add(scrollPane, BorderLayout.CENTER);
        add(panel, BorderLayout.SOUTH);

        setVisible(true); // Hace visible la ventana
    }

    // Iniciar el chat privado en un hilo separado
    private void startPrivateChat() {
        new Thread(() -> {
            try {
                while (true) {
                    String message = input.readUTF(); // Lee mensajes del servidor
                    if (message.startsWith("FILE:")) {
                        String[] fileInfo = message.split(":");
                        String fileName = fileInfo[1];
                        long fileSize = Long.parseLong(fileInfo[2]);

                        // Preguntar al usuario si acepta el archivo
                        int response = JOptionPane.showConfirmDialog(this, 
                                "¿Aceptar archivo " + fileName + " (" + fileSize / (1024 * 1024) + " MB)?",
                                "Solicitud de archivo", JOptionPane.YES_NO_OPTION);

                        if (response == JOptionPane.YES_OPTION) {
                            receiveFile(fileName, fileSize); // Recibe el archivo si el usuario acepta
                        } else {
                            output.writeUTF("Archivo rechazado"); // Informa al servidor que el archivo fue rechazado
                        }
                    } else {
                        chatArea.append(message + "\n"); // Añade el mensaje al área de chat
                    }
                }
            } catch (IOException e) {
                closeResources(); // Cerrar recursos en caso de error
            }
        }).start();
    }

    // Manejar el evento de acción (enviar mensaje)
    @Override
    public void actionPerformed(ActionEvent e) {
        sendMessage(); // Llama a la función para enviar el mensaje
    }

    // Enviar un mensaje
    private void sendMessage() {
        String message = messageField.getText().trim(); // Obtiene el texto del campo de mensaje
        if (!message.isEmpty()) {
            try {
                String encryptedMessage = EncryptionChat.encrypt(sender + ": " + message, secretKey); // Encripta el mensaje
                output.writeUTF("PRIVATE:" + recipient + ":" + encryptedMessage); // Especifica que es un mensaje privado
                messageField.setText(""); // Limpia el campo de mensaje
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
        int result = fileChooser.showOpenDialog(this); // Abre el diálogo para seleccionar un archivo

        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            if (file.length() <= 50 * 1024 * 1024) {  // Limitar el tamaño del archivo a 50 MB
                try (FileInputStream fis = new FileInputStream(file)) {
                    output.writeUTF("FILE:" + file.getName() + ":" + file.length()); // Envía la información del archivo
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = fis.read(buffer)) != -1) {
                        output.write(buffer, 0, bytesRead); // Envía el archivo en bloques
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

    // Recibir un archivo
    private void receiveFile(String fileName, long fileSize) {
        try {
            // Definir la ruta de descarga en la carpeta de descargas del sistema
            String userHome = System.getProperty("user.home");
            File downloadDir = new File(userHome, "Downloads");
            File file = new File(downloadDir, fileName);
    
            try (FileOutputStream fos = new FileOutputStream(file)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                long totalBytesRead = 0;
    
                while (totalBytesRead < fileSize && (bytesRead = input.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead); // Escribe el archivo en bloques
                    totalBytesRead += bytesRead;
                }
    
                chatArea.append("Archivo recibido: " + file.getAbsolutePath() + "\n");
            }
        } catch (IOException e) {
            chatArea.append("Error recibiendo el archivo.\n");
            e.printStackTrace();
        }
    }

    // Cerrar recursos (streams y socket)
    private void closeResources() {
        try {
            if (input != null) input.close(); // Cierra el input stream
            if (output != null) output.close(); // Cierra el output stream
            if (socket != null) socket.close(); // Cierra el socket
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
