package ru.postlife.javaChatServer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ClientHandler {
    private Socket socket;
    private Server server;
    private String username;
    private DataInputStream in;
    private DataOutputStream out;

    public ClientHandler(Server server, Socket socket) {
        try {
            this.socket = socket;
            this.server = server;
            this.in = new DataInputStream(socket.getInputStream());
            this.out = new DataOutputStream(socket.getOutputStream());

            new Thread(() -> {
                try {
                    while (true) {
                        String message = in.readUTF();
                        if (message.startsWith("/auth ")) {
                            username = message.split("\\s+", 2)[1];
                            if (!this.server.checkNicknameAvailability(username)) {
                                sendMessage("SERVER: nickname '" + username + "' isn't available!");
                            } else {
                                sendMessage("/authok");
                                sendMessage("SERVER: Welcome to chat");
                                this.server.subscribe(this);
                                break;
                            }
                        } else {
                            sendMessage("SERVER: Authorization is required");
                        }
                    }
                    while (true) {
                        String message = in.readUTF();
                        if (message.startsWith("/")) {
                            continue;
                        }
                        this.server.broadcastMessage(username + ": " + message);
                    }
                } catch (IOException e) {
                    System.out.println("Client '" + this.getUsername() + "' disconnected");
                } finally {
                    this.server.unsubscribe(this);
                }
            }).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMessage(String message) {
        try {
            out.writeUTF(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getUsername() {
        return username;
    }
}
