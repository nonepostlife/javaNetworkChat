package ru.postlife.javaChatServer;

public interface AuthService {
    void start();

    void stop();

    String getNickByLoginPass(String login, String password);

    String changeNickname(String oldNickname, String newNickname);

    String registerNewUser(String login, String password, String nickname);
}
