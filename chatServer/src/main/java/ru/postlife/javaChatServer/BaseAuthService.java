package ru.postlife.javaChatServer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class BaseAuthService implements AuthService {
    private class Entry {
        private String login;
        private String password;
        private String nick;

        public Entry(String login, String pass, String nick) {
            this.login = login;
            this.password = pass;
            this.nick = nick;
        }
    }

    private List<Entry> entries;
    private static final Logger logger;
    static {
        logger = LogManager.getLogger(BaseAuthService.class);
    }

    public BaseAuthService() {
        entries = new ArrayList<>();
        entries.add(new Entry("login1", "pass1", "nick1"));
        entries.add(new Entry("login2", "pass2", "nick2"));
        entries.add(new Entry("login3", "pass3", "nick3"));
    }

    @Override
    public void start() {
        logger.info("Base service authentication is run");
    }

    @Override
    public void stop() {
        logger.info("Base service authentication is stop");
    }

    @Override
    public String getNickByLoginPass(String login, String pass) {
        for (Entry o : entries) {
            if (o.login.equals(login) && o.password.equals(pass)) return o.nick;
        }
        return null;
    }

    @Override
    public String changeNickname(String oldNickname, String newNickname) {
        for (Entry e : entries) {
            if (e.nick.equals(oldNickname)) {
                e.nick = newNickname;
                return "/changeok";
            }
        }
        return "/changefail";
    }

    @Override
    public String registerNewUser(String login, String password, String nickname) {
        for (Entry entry : entries) {
            if (entry.login.equals(login)) {
                return "/registerfail login is already in use!";
            }
            if (entry.nick.equals(nickname)) {
                return "/registerfail nickname is already in use!";
            }
        }
        entries.add(new Entry(login, password, nickname));
        return "/registerok Registration successful";
    }
}
