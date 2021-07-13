package ru.postlife.javaChatServer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
    private static final Logger logger;

    static {
        logger = LogManager.getLogger(BaseAuthService.class);
    }

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
            this.username = "'unknown'";
        } catch (IOException e) {
            logger.warn("ClientHandler was not created - " + e.getMessage());
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
    public void logic() {
        try {
            while (!consumeAuthorizeMessage(in.readUTF())) ;
            if (socket.isClosed()) {
                return;
            }
            while (consumeRegularMessage(in.readUTF())) ;
        } catch (IOException e) {
            logger.warn("Failed to receive message - " + e.getMessage());
            e.printStackTrace();
        } finally {
            //System.out.println("Client '" + this.getUsername() + "' disconnected");
            logger.info("Client '" + this.getUsername() + "' disconnected");
            server.unsubscribe(this);
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
            logger.info(username + " send command - " + message);
            if (message.equals("/exit")) {
                sendMessage("/exit");
                logger.info("to " + username + ": SERVER: /exit");
                return false;
            }
            if (message.startsWith("/changenickname ")) {
                String[] tokens = message.split("\\s+", 3);
                if (tokens[1].equals(tokens[2])) {
                    sendMessage("SERVER: old nickname is the same as new nickname");
                    logger.info("to " + username + ": SERVER: old nickname is the same as new nickname");
                    return true;
                }
                String result = server.getAuthService().changeNickname(tokens[1], tokens[2]);
                if (result.equals("/changeok")) {
                    username = tokens[2];
                    sendMessage(result + " " + username);
                    server.broadcastMessage("SERVER: " + tokens[1] + " changed his nickname to " + tokens[2]);
                    logger.info("to " + username + ": SERVER: " + result + " " + username);
                    logger.info("SERVER: " + tokens[1] + " changed his nickname to " + tokens[2]);
                    server.broadcastClientList();
                } else {
                    sendMessage("SERVER: fail change nickname - " + result);
                }
                return true;
            }
            if (message.startsWith("/w ")) {
                String[] tokens = message.split("\\s+", 3);
                server.sendPersonalMessage(this, tokens[1], tokens[2]);
            }
            return true;
        }
        logger.info(username + " send message - " + message);
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
        if (message.startsWith("/")) {
            logger.info(username + " send command - " + message);
            if (message.startsWith("/register ")) {
                String[] tokens = message.split("\\s+");
                if (tokens.length != 4) {
                    sendMessage("SERVER: spaces can't be used in fields");
                    logger.info("to " + username + ": SERVER: spaces can't be used in fields");
                    return false;
                }
                String result = server.getAuthService().registerNewUser(tokens[1], tokens[2], tokens[3]);
                logger.info("to " + username + ": " + result);
                sendMessage(result);
                return false;
            }
            if (message.startsWith("/auth ")) {
                String[] tokens = message.split("\\s+");
                if (tokens.length < 3) {
                    sendMessage("SERVER: you didn't provide login or password");
                    logger.info("to " + username + ": SERVER: you didn't provide login or password");
                    return false;
                }
                if (tokens.length > 3) {
                    sendMessage("SERVER: login must contain ONE word");
                    logger.info("to " + username + ": SERVER: login must contain ONE word");
                    return false;
                }
                String selectedUsername = server.getAuthService().getNickByLoginPass(tokens[1], tokens[2]);
                if (selectedUsername != null) {
                    if (!this.server.checkNicknameAvailability(selectedUsername)) {
                        sendMessage("SERVER: account is already in use!");
                        logger.info("to " + username + ": SERVER: account is already in use!");
                        return false;
                    }
                    username = selectedUsername;
                    sendMessage("/authok " + username);
                    sendMessage("SERVER: welcome to chat");
                    logger.info("to " + username + ": " + "/authok");
                    logger.info("to " + username + ": SERVER: welcome to chat!");
                    server.subscribe(this);
                    return true;
                } else {
                    sendMessage("SERVER: wrong login or password");
                    logger.info("to " + username + ": SERVER: wrong login or password");
                    return false;
                }
            }
            if (message.equals("/exit")) {
                sendMessage("/exit");
                logger.info("to " + username + ": /exit");
                closeConnection();
                return true;
            }
        } else {
            sendMessage("SERVER: authentication required");
            logger.info("to " + username + ": SERVER: authentication required");
        }
        return false;
    }

    /**
     * метод закрытия соединения с клиентским приложением (сокет, вход. выход. потоков)
     */
    public void closeConnection() {
        try {
            if (in != null) {
                in.close();
                logger.info(this.username + ": in is closed - true");
            }
        } catch (IOException e) {
            logger.warn(this.username + ": in is closed - false");
            logger.warn(e.getMessage());
            e.printStackTrace();
        }
        try {
            if (out != null) {
                out.close();
                logger.info(this.username + ": out is closed - true");
            }
        } catch (IOException e) {
            logger.warn(this.username + ": out is closed - false");
            logger.warn(e.getMessage());
            e.printStackTrace();
        }
        try {
            if (socket != null) {
                socket.close();
                logger.info(this.username + ": socket is closed - true");
            }
        } catch (IOException e) {
            logger.warn(this.username + ": socket is closed - false");
            logger.warn(e.getMessage());
            e.printStackTrace();
        }
    }
}
