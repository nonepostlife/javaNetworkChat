package ru.postlife.javaChatServer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Класс поведения сервера
 */
public class Server {
    private final int PORT = 8189;
    private List<ClientHandler> clientsList;
    private AuthService authService;
    private static final Logger logger;

    static {
        logger = LogManager.getLogger(Server.class);
    }

    /**
     * конструктор класса Server
     */
    public Server() {
        try {
            this.clientsList = new ArrayList<>();
            // открытие сокета
            ServerSocket serverSocket = new ServerSocket(PORT);
            System.out.println("Server has been started. Wait connect..");
            logger.info("Server has been started. Wait connect..");
            authService = new DatabaseAuthService();
            authService.start();
            // жизненный цикл сервера, подключение новых клиентов
            while (true) {
                Socket socket = serverSocket.accept();
                ClientHandler c = new ClientHandler(this, socket);
                logger.info("New client connected");
            }
        } catch (IOException e) {
            logger.warn("Server: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (authService != null) {
                authService.stop();
            }
            logger.info("Server has been stopped.");
        }
    }

    /**
     * метод добавления нового пользователя в список пользователей
     * и оповещения остальных пользователей о подключении нового пользователя
     *
     * @param c добавляемый пользователь
     */
    public synchronized void subscribe(ClientHandler c) {
        broadcastMessage("SERVER: " + c.getUsername() + " joined to chat!");
        logger.info("SERVER: " + c.getUsername() + " joined to chat!");
        clientsList.add(c);
        broadcastClientList();
    }

    /**
     * метод удаления пользователя из список пользователей
     * и оповещения остальных пользователей об отключении пользователя
     *
     * @param c удаляемый пользователь
     */
    public synchronized void unsubscribe(ClientHandler c) {
        clientsList.remove(c);
        broadcastMessage("SERVER: " + c.getUsername() + " left from chat!");
        logger.info("SERVER: " + c.getUsername() + " left from chat!");
        broadcastClientList();
    }

    /**
     * широковещательная отправка сообщения
     *
     * @param message отправляемое сообщение
     */
    public synchronized void broadcastMessage(String message) {
        for (ClientHandler client : clientsList) {
            client.sendMessage(message);
        }
    }

    /**
     * формирование списка клиентов
     */
    public synchronized void broadcastClientList() {
        ArrayList<String> list = new ArrayList<>();
        for (ClientHandler client : clientsList) {
            list.add(client.getUsername());
        }
        Collections.sort(list);
        list.add(0, "/clients_list ");
        String clientsListString = list.toString().replaceAll("[]\\[,]", "");
        broadcastMessage(clientsListString);
    }

    /**
     * метод проверки доступности никнейма среди других клиентов
     *
     * @param username проверяемое имя пользователя
     * @return возвращает true если проверяеоме имя пользователя не используется
     * возвращает false в противном случае
     */
    public synchronized boolean checkNicknameAvailability(String username) {
        for (ClientHandler client : clientsList) {
            if (username.equalsIgnoreCase(client.getUsername())) {
                return false;
            }
        }
        return true;
    }

    /**
     * метод отправки личного сообщения пользователю
     *
     * @param sender           отправитель
     * @param receiverUsername имя получателя сообщения
     * @param message          отправляемое сообщение
     */
    public synchronized void sendPersonalMessage(ClientHandler sender, String receiverUsername, String message) {
        if (sender.getUsername().equalsIgnoreCase(receiverUsername)) {
            sender.sendMessage("SERVER: you cannot send private message to yourself");
            logger.info("to " + sender.getUsername() + ": SERVER: you cannot send private message to yourself");
            return;
        }
        for (ClientHandler client : clientsList) {
            if (client.getUsername().equalsIgnoreCase(receiverUsername)) {
                client.sendMessage("From " + sender.getUsername() + ": " + message);
                sender.sendMessage("to " + receiverUsername + ": " + message);
                return;
            }
        }
        sender.sendMessage("SERVER: user " + receiverUsername + " is offline");
        logger.info("to " + sender.getUsername() + "SERVER: user " + receiverUsername + " is offline");
    }

    public AuthService getAuthService() {
        return authService;
    }
}
