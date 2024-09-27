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
        setSize(400, 400);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        JScrollPane scrollPane = new JScrollPane(chatArea);
        messageField = new JTextField();
        sendButton = new JButton("Send");
        sendButton.addActionListener(this);

        add(scrollPane, BorderLayout.CENTER);
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(messageField, BorderLayout.CENTER);
        panel.add(sendButton, BorderLayout.EAST);
        add(panel, BorderLayout.SOUTH);

        setVisible(true);
    }

    private void startChat() {
        new Thread(() -> {
            try {
                while (true) {
                    String message = input.readUTF();
                    chatArea.append(message + "\n");
                }
            } catch (IOException e) {
                closeResources();
            }
        }).start();

        // Añadir un listener para enviar un mensaje de desconexión al cerrar la ventana
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

        // Añadir un KeyListener para enviar el mensaje al presionar Enter
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
