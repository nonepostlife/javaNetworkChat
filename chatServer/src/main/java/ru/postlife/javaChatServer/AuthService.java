package ru.postlife.javaChatServer;

import java.sql.SQLException;

public interface AuthService {
    void start();

    void stop();

    String getNickByLoginPass(String login, String pass);
}
