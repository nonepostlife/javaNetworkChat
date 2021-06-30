package ru.postlife.javaChatServer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

/**
 * Класс обработчика пользователей
 */
public class ClientHandler {
    private Socket socket;
    private Server server;
    private String username;
    private DataInputStream in;
    private DataOutputStream out;

    /**
     * метод получения имени польователя
     *
     * @return строка с именем пользователя
     */
    public String getUsername() {
        return username;
    }

    /**
     * констурктор класса ClientHandler
     *
     * @param server поле для хранения ссылки на сервер
     * @param socket поле сокета конкретного пользователя
     */
    public ClientHandler(Server server, Socket socket) {
        try {
            this.socket = socket;
            this.server = server;
            this.in = new DataInputStream(socket.getInputStream());
            this.out = new DataOutputStream(socket.getOutputStream());
            new Thread(() -> logic()).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * отправка сообщения пользователю
     *
     * @param message отправляемое сообщение
     */
    public void sendMessage(String message) {
        try {
            out.writeUTF(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * логика поведения обработчика
     * принимет сообщения от клиентского приложения
     */
    private void logic() {
        try {
            while (!consumeAuthorizeMessage(in.readUTF())) ;
            if (socket.isClosed()) {
                return;
            }
            while (consumeRegularMessage(in.readUTF())) ;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            System.out.println("Client '" + this.getUsername() + "' disconnected");
            this.server.unsubscribe(this);
            closeConnection();
        }
    }

    /**
     * метод для приема сообщений от уже авторизованного пользователя
     *
     * @param message приниммаемое сообщение
     * @return возвращает true при успешном общении с пользователем
     * возвращает false если от пользоваетля пришел запрос
     * для закрытия соединения
     */
    private boolean consumeRegularMessage(String message) {
        if (message.startsWith("/")) {
            if (message.equals("/exit")) {
                sendMessage("/exit");
                return false;
            }
            if (message.startsWith("/w ")) {
                String[] tokens = message.split("\\s+", 3);
                server.sendPersonalMessage(this, tokens[1], tokens[2]);
            }
            return true;
        }
        server.broadcastMessage(username + ": " + message);
        return true;
    }

    /**
     * метод для приема сообщений от неавторизованного пользователя
     *
     * @param message приниммаемое сообщение
     * @return возвращает true при успешной авторизации пользователя
     * возвращает false если пользователь не был авторизован
     */
    private boolean consumeAuthorizeMessage(String message) {
        if (message.startsWith("/auth ")) {
            String[] tokens = message.split("\\s+");
            if (tokens.length < 3) {
                sendMessage("SERVER: You didn't provide login or password");
                return false;
            }
            if (tokens.length > 3) {
                sendMessage("SERVER: login must contain ONE word");
                return false;
            }

            String selectedUsername = server.getAuthService().getNickByLoginPass(tokens[1], tokens[2]);
            if (selectedUsername != null) {
                if (!this.server.checkNicknameAvailability(selectedUsername)) {
                    sendMessage("SERVER: account is already in use!");
                    return false;
                }
                username = selectedUsername;
                sendMessage("/authok " + username);
                sendMessage("SERVER: welcome to chat");
                server.subscribe(this);
                return true;
            } else {
                sendMessage("SERVER: wrong login or password");
                return false;
            }
        } else if (message.equals("/exit")) {
            sendMessage("/exit");
            closeConnection();
            return true;
        } else {
            sendMessage("SERVER: authentication required");
            return false;
        }
    }

    /**
     * метод закрытия соединения с клиентским приложением (сокет, вход. выход. потоков)
     */
    public void closeConnection() {
        try {
            if (in != null) {
                in.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            if (out != null) {
                out.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}