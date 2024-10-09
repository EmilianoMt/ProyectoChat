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
    private JTextArea PrivatechatArea;
    private JTextField messageField;
    private JButton sendButton, fileButton;
    private String sender;
    private String recipient;
    private SecretKey secretKey;
    private MessageReceiver messageReceiver;
    private Thread receiverThread;

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
    
        initializeUIPrivateChat(); // Inicializa la interfaz
        startPrivateChat(); // Inicia el chat privado
    
        messageReceiver = new MessageReceiver(input, PrivatechatArea, secretKey);
        receiverThread = new Thread(messageReceiver);
        receiverThread.start();
    }

    // Inicializar la interfaz de usuario
    private void initializeUIPrivateChat() {
        setTitle("Chat Privado con " + recipient); // Establece el título de la ventana
        setSize(400, 300); // Establece el tamaño de la ventana
        setDefaultCloseOperation(DISPOSE_ON_CLOSE); // Define la acción de cierre

        PrivatechatArea = new JTextArea();
        PrivatechatArea.setEditable(false); // Hace que el área de chat no sea editable
        PrivatechatArea.setLineWrap(true); // Habilita el ajuste de línea
        JScrollPane scrollPane = new JScrollPane(PrivatechatArea); // Añade un scroll al área de chat

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
                    String message = input.readUTF(); // Lee el mensaje del servidor
                    if (message.startsWith("PRIVATE:")) {
                        PrivatechatArea.append(recipient + ": " + message);
                    } else
                    if (message.startsWith("FILE:")) {
                        String[] parts = message.split(":", 3); // Separa la cadena
                        if (parts.length == 3) {
                            String fileName = parts[1];
                            long fileSize = Long.parseLong(parts[2]);
                            receiveFile(fileName, fileSize); // Recibe el archivo
                        }
                    }
                }
            } catch (IOException e) {
                closeResources();
            }
        }).start();
        
    }

    // private void decryptAndDisplayMessage(String encryptedMessage) {
    //     try {
    //         String decryptedMessage = EncryptionChat.decrypt(encryptedMessage, secretKey); // Desencripta el mensaje
    //         PrivatechatArea.append(decryptedMessage + "\n"); // Muestra el mensaje desencriptado
    //     } catch (Exception ex) {
    //         PrivatechatArea.append("Error desencriptando el mensaje.\n");
    //         ex.printStackTrace();
    //     }
    // }

    // Manejar el evento de acción (enviar mensaje)
    @Override
    public void actionPerformed(ActionEvent e) {
        sendMessage();
    }

    // Enviar un mensaje
private void sendMessage() {
    try {
        String msg = messageField.getText();
        String encryptedMessage;
        try {
            encryptedMessage = EncryptionChat.encrypt(msg, secretKey); // Encripta el mensaje
        } catch (Exception ex) {
            PrivatechatArea.append("Error encriptando el mensaje.\n");
            ex.printStackTrace();
            return; // Salir del método si hay un error en la encriptación
        }
        output.writeUTF("PRIVATE:" + recipient + ":" + encryptedMessage); // Envía el mensaje encriptado
        messageField.setText("");
        PrivatechatArea.append("Yo: " + msg + "\n"); // Muestra el mensaje en el área de chat
    } catch (IOException ex) {
        ex.printStackTrace();
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
                    PrivatechatArea.append("Archivo enviado: " + file.getName() + "\n");
                } catch (IOException e) {
                    PrivatechatArea.append("Error enviando el archivo.\n");
                    e.printStackTrace();
                }
            } else {
                PrivatechatArea.append("El archivo supera el límite de 50 MB.\n");
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
    
                PrivatechatArea.append("Archivo recibido: " + file.getAbsolutePath() + "\n");
            }
        } catch (IOException e) {
            PrivatechatArea.append("Error recibiendo el archivo.\n");
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
