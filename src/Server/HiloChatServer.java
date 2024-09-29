package src.Server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class HiloChatServer implements Runnable {
    private Socket socket;
    private final Vector<Socket> vector;
    private DataInputStream netIn;
    private DataOutputStream netOut;
    private static List<String> usuarios = new ArrayList<>();
    private String username;

    // Constructor que inicializa el socket y el vector de sockets
    public HiloChatServer(Socket socket, Vector<Socket> vector) {
        this.socket = socket;
        this.vector = vector;
    }

    // Método estático para obtener la lista de usuarios
    public static List<String> getUsuarios() {
        return usuarios;
    }

    // Inicializa los streams de entrada y salida, y añade el usuario a la lista
    private void initStreams() throws IOException {
        netIn = new DataInputStream(socket.getInputStream());
        username = netIn.readUTF();
        synchronized (usuarios) {
            usuarios.add(username);
        }
    }

    // Envía un mensaje a todos los clientes conectados
    private void sendMsgToAll(String msg) {
        synchronized (vector) {
            for (Socket soc : vector) {
                try {
                    netOut = new DataOutputStream(soc.getOutputStream());
                    netOut.writeUTF(msg);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // Envía la lista de usuarios a todos los clientes conectados
    private void sendUserListToAll() {
        synchronized (vector) {
            String userList = "USERS:" + String.join(",", usuarios);
            for (Socket soc : vector) {
                try {
                    netOut = new DataOutputStream(soc.getOutputStream());
                    netOut.writeUTF(userList);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // Método principal del hilo, maneja la comunicación con el cliente
    @Override
    public void run() {
        try {
            initStreams();
            sendMsgToAll("Server: " + username + " se unió al chat.");
            sendUserListToAll();
    
            // Bucle para recibir y enviar mensajes
            while (true) {
                try {
                    String msg = netIn.readUTF();
                    sendMsgToAll(msg);
                } catch (IOException ioe) {
                    System.out.println("Client disconnected: " + username);
                    break; // Salir del bucle cuando el cliente se desconecta
                }
            }
            
        } catch (IOException ioe) {
            System.out.println("Error initializing streams for: " + username);
        } finally {
            // Elimina al usuario y cierra el socket en caso de desconexión
            synchronized (usuarios) {
                usuarios.remove(username);
            }
            synchronized (vector) {
                vector.remove(socket);
                sendUserListToAll();
            }
            closeResources();
        }
    }
    
    

    // Cierra los recursos de entrada, salida y el socket
    private void closeResources() {
        try {
            if (netIn != null) netIn.close();
            if (netOut != null) netOut.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
