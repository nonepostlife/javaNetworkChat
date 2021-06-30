package ru.postlife.javaChatServer;

import java.sql.*;

import org.mindrot.jbcrypt.*;

public class DatabaseAuthService implements AuthService {
    private String jdbcURL = "jdbc:mysql://localhost:3306/netchat";
    private String username = "root";
    private String password = "root";
    private Connection connection;
    private Statement statement;

    @Override
    public void start() {
        try {
            DriverManager.registerDriver(new com.mysql.cj.jdbc.Driver());
            connection = DriverManager.getConnection(jdbcURL, username, password);
            statement = connection.createStatement();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    @Override
    public void stop() {
        try {
            if (statement != null) {
                statement.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getNickByLoginPass(String login, String password) {
        try (ResultSet rs = statement.executeQuery("Select user_nickname, user_password from user " +
                "where user_login = '" + login + "'")) {
            String nickname = "";
            String stored_hash = "";

            while (rs.next()) {
                nickname = rs.getString("user_nickname");
                stored_hash = rs.getString("user_password");
            }

            if (nickname != "" && checkPassword(password, stored_hash)) {
                return nickname;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // Define the BCrypt workload to use when generating password hashes. 10-31 is a valid value.
    private static int workload = 12;

    /**
     * This method can be used to generate a string representing an account password
     * suitable for storing in a database. It will be an OpenBSD-style crypt(3) formatted
     * hash string of length=60
     * The bcrypt workload is specified in the above static variable, a value from 10 to 31.
     * A workload of 12 is a very reasonable safe default as of 2013.
     * This automatically handles secure 128-bit salt generation and storage within the hash.
     *
     * @param password_plaintext The account's plaintext password as provided during account creation,
     *                           or when changing an account's password.
     * @return String - a string of length 60 that is the bcrypt hashed password in crypt(3) format.
     */
    public String hashPassword(String password_plaintext) {
        String salt = BCrypt.gensalt(workload);
        String hashed_password = BCrypt.hashpw(password_plaintext, salt);

        return (hashed_password);
    }

    /**
     * This method can be used to verify a computed hash from a plaintext (e.g. during a login
     * request) with that of a stored hash from a database. The password hash from the database
     * must be passed as the second variable.
     *
     * @param password_plaintext The account's plaintext password, as provided during a login request
     * @param stored_hash        The account's stored password hash, retrieved from the authorization database
     * @return boolean - true if the password matches the password of the stored hash, false otherwise
     */
    public boolean checkPassword(String password_plaintext, String stored_hash) {
        boolean password_verified = false;

        if (null == stored_hash || !stored_hash.startsWith("$2a$"))
            throw new java.lang.IllegalArgumentException("Invalid hash provided for comparison");

        password_verified = BCrypt.checkpw(password_plaintext, stored_hash);

        return (password_verified);
    }
}
