package src.Server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

public class HiloChatServer implements Runnable {
    private Socket socket;
    private final Vector<Socket> vector;
    private DataInputStream netIn;
    private DataOutputStream netOut;
    private static List<String> usuarios = new ArrayList<>();
    private String username;
    private static HashMap<String, SecretKey> sharedKeysMap = new HashMap<>();

    // Constructor
    public HiloChatServer(Socket socket, Vector<Socket> vector) {
        this.socket = socket;
        this.vector = vector;
    }

    // Devuelve la lista de usuarios conectados
    public static List<String> getUsuarios() {
        return usuarios;
    }

    // Inicializa los streams de entrada y salida y obtiene el nombre de usuario del cliente
    private void initStreams() throws IOException {
        netIn = new DataInputStream(socket.getInputStream());
        netOut = new DataOutputStream(socket.getOutputStream());
        username = netIn.readUTF(); // Lee el nombre de usuario del cliente
        synchronized (usuarios) {
            usuarios.add(username); // Añade el nombre de usuario a la lista de usuarios conectados
        }
    }

    // Envía un mensaje a todos los clientes conectados
    private void sendMsgToAll(String msg) {
        synchronized (vector) {
            System.out.println("Sockets activos: " + vector.size());
            for (Socket soc : vector) {
                if (!soc.isClosed()) {
                    try {
                        DataOutputStream out = new DataOutputStream(soc.getOutputStream());
                        out.writeUTF(msg); // Envía el mensaje a cada cliente
                        System.out.println("Mensaje enviado a: " + soc);
                    } catch (IOException e) {
                        e.printStackTrace();
                        try {
                            soc.close();
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            }
            vector.removeIf(Socket::isClosed); // Elimina los sockets cerrados del vector
        }
    }
    
    // Envía la lista de usuarios conectados a todos los clientes
    private void sendUserListToAll() {
        synchronized (vector) {
            String userList = "USERS:" + String.join(",", usuarios); // Crea una cadena con la lista de usuarios
            System.out.println("Enviando lista de usuarios: " + userList);
            for (Socket soc : vector) {
                if (!soc.isClosed()) {
                    try {
                        DataOutputStream out = new DataOutputStream(soc.getOutputStream());
                        out.writeUTF(userList); // Envía la lista de usuarios a cada cliente
                    } catch (IOException e) {
                        e.printStackTrace();
                        try {
                            soc.close();
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            }
            vector.removeIf(Socket::isClosed); // Elimina los sockets cerrados del vector
        }
    }
    
    // Método principal que se ejecuta cuando el hilo comienza
    @Override
    public void run() {
        try {
            initStreams();  // Inicializa los streams
            
            // Asegura que el socket se agregue solo una vez
            synchronized (vector) {
                if (!vector.contains(socket)) {
                    vector.add(socket); // Añade el socket al vector si no está ya presente
                    System.out.println("Socket añadido: " + socket);
                } else {
                    System.out.println("Socket duplicado: " + socket);
                }
            }

            sendMsgToAll("Server: " + username + " se unió al chat."); // Notifica a todos los clientes que un nuevo usuario se ha unido
            sendUserListToAll(); // Envía la lista de usuarios a todos los clientes
            
            while (true) {
                try {
                    String msg = netIn.readUTF();  // Mensaje recibido desde el cliente
                    if (msg.startsWith("PRIVATE:")) {
                        handlePrivateMessage(msg); // Maneja los mensajes privados
                    } else if (msg.startsWith("REQUEST_PRIVATE_KEY:")) {
                        handlePrivateKeyRequest(msg); // Maneja las solicitudes de clave privada
                    } else {
                        sendMsgToAll(msg);  // Envío del mensaje a todos los clientes
                    }
                } catch (IOException ioe) {
                    System.out.println("Client disconnected: " + username);
                    break; // Sale del bucle si el cliente se desconecta
                }
            }
        } catch (IOException ioe) {
            System.out.println("Error initializing streams for: " + username);
        } finally {
            synchronized (usuarios) {
                usuarios.remove(username); // Elimina el usuario de la lista de usuarios conectados
            }
            synchronized (vector) {
                vector.remove(socket); // Elimina el socket del vector
                sendUserListToAll(); // Envía la lista de usuarios actualizada a todos los clientes
            }
            closeResources(); // Cierra los recursos
        }
    }

    // Maneja los mensajes privados entre usuarios
    private void handlePrivateMessage(String msg) {
        String[] parts = msg.split(":", 3);
        String recipient = parts[1];
        String encryptedMessage = parts[2];

        synchronized (vector) {
            for (Socket soc : vector) {
                try {
                    DataOutputStream out = new DataOutputStream(soc.getOutputStream());
                    // Envía el mensaje solo al destinatario
                    if (usuarios.contains(recipient)) {
                        out.writeUTF("PRIVATE:" + username + ":" + encryptedMessage);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // Maneja la solicitud de clave privada
    private void handlePrivateKeyRequest(String msg) {
        String[] parts = msg.split(":", 2);
        String recipient = parts[1];
        
        // if (!usuarios.contains(username)) {
        //     System.out.println("Error: Usuario no encontrado.");
        //     return;
        // }

        try {
            // Busca si ya hay una clave compartida entre los dos usuarios
            SecretKey sharedKey = getOrGenerateSharedKey(username, recipient);

            // Envía la clave a ambos usuarios (solicitante y receptor)
            sendPrivateKey(username, sharedKey);
            sendPrivateKey(recipient, sharedKey);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Obtiene o genera una clave compartida entre dos usuarios
    private SecretKey getOrGenerateSharedKey(String user1, String user2) throws Exception {
        // Genera un identificador único para el par de usuarios
        String keyIdentifier = user1.compareTo(user2) < 0 ? user1 + ":" + user2 : user2 + ":" + user1;
        
        // Si ya existe una clave compartida entre los usuarios, la devuelve
        if (sharedKeysMap.containsKey(keyIdentifier)) {
            return sharedKeysMap.get(keyIdentifier);
        }
        
        // Si no existe, genera una nueva clave y la guarda
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(128);
        SecretKey newKey = keyGen.generateKey();
        sharedKeysMap.put(keyIdentifier, newKey);
        return newKey;
    }

    // Envía la clave secreta a un usuario
    private void sendPrivateKey(String username, SecretKey key) throws IOException {
        for (Socket soc : vector) {
            if (!soc.isClosed() && usuarios.contains(username)) {
                DataOutputStream out = new DataOutputStream(soc.getOutputStream());
                String encodedKey = Base64.getEncoder().encodeToString(key.getEncoded());
                out.writeUTF("PRIVATE_KEY:" + encodedKey); // Envía la clave privada codificada en Base64
                System.out.println(encodedKey);
            }
        }
    }

    // Cierra los recursos de entrada, salida y el socket
    public void closeResources() {
        try {
            if (netIn != null) netIn.close();
            if (netOut != null) netOut.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}                                                                             