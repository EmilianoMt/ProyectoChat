package src.Client;

import java.io.DataInputStream;
import java.io.IOException;
import javax.crypto.SecretKey;
import javax.swing.JTextArea;

public class MessageReceiver implements Runnable {
    private DataInputStream input;
    private JTextArea chatArea;
    private SecretKey secretKey;

    public MessageReceiver(DataInputStream input, JTextArea chatArea, SecretKey secretKey) {
        this.input = input;
        this.chatArea = chatArea;
        this.secretKey = secretKey;
    }

    @Override
    public void run() {
        try {
            while (true) {
                String message = input.readUTF();
                if (message.startsWith("PRIVATE:")) {
                    String[] parts = message.split(":", 3);
                    if (parts.length == 3) {
                        String sender = parts[1];
                        String encryptedMessage = parts[2];
                        try {
                            String decryptedMessage = EncryptionChat.decrypt(encryptedMessage, secretKey); // Desencripta el mensaje
                            chatArea.append(sender + ": " + decryptedMessage + "\n");
                        } catch (Exception ex) {
                            chatArea.append("Error desencriptando el mensaje de " + sender + ".\n");
                            ex.printStackTrace();
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

