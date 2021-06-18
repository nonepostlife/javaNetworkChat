package ru.postlife.javaChatServer;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Server {
    private List<ClientHandler> clientsList;

    public Server() {
        try {
            this.clientsList = new ArrayList<>();
            ServerSocket serverSocket = new ServerSocket(8189);
            System.out.println("Server has been started. Wait connect..");

            while (true) {
                Socket socket = serverSocket.accept();
                ClientHandler c = new ClientHandler(this, socket);
                System.out.println("New client connected");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void subscribe(ClientHandler c) {
        clientsList.add(c);
        notifyOthersParticipants(c.getUsername() + " присоединился к чату!", c);
    }

    public synchronized void unsubscribe(ClientHandler c) {
        clientsList.remove(c);
        notifyOthersParticipants(c.getUsername() + " покинул чат!", c);
    }

    public synchronized void broadcastMessage(String message) {
        for (ClientHandler client : clientsList) {
            client.sendMessage(message);
        }
    }

    public synchronized void notifyOthersParticipants(String message, ClientHandler c) {
        for (ClientHandler client : clientsList) {
            if (client.equals(c)) {
                continue;
            }
            client.sendMessage(message);
        }
    }

    public boolean checkNicknameAvailability(String username) {
        for (ClientHandler client : clientsList) {
            if (username.equals(client.getUsername())) {
                return false;
            }
        }
        return true;
    }
}
