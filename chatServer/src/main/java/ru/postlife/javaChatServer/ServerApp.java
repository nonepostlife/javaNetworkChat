package ru.postlife.javaChatServer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerApp {
    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(8189)) {
            System.out.println("Server has been started. Wait connect..");
                while (true) {
                    Socket socket = serverSocket.accept();
                    System.out.println("Client connected");

                    Thread newClientConnection = new Thread(() -> {
                        try {
                            DataInputStream in = new DataInputStream(socket.getInputStream());
                            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                            while (true) {
                                String message = in.readUTF();
                                if(message.equals("/end"))
                                    break;
                                System.out.println(message);
                                out.writeUTF("ECHO: " + message);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
                    newClientConnection.start();
                }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
