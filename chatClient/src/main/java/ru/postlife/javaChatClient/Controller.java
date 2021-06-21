package ru.postlife.javaChatClient;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * Класс управления окном приложения
 */
public class Controller implements Initializable {
    @FXML
    TextArea chatArea;
    @FXML
    TextField textMessageArea;
    @FXML
    HBox msgPanel, authPanel;
    @FXML
    TextField usernameField;
    @FXML
    ListView<String> clientsListView;

    private Socket socket;
    private String username;
    private DataInputStream in;
    private DataOutputStream out;

    /**
     * метод инициализации окна приложения
     */
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        clientsListView.setCellFactory(stringListView -> new Main.CellFactory());
    }

    /**
     * метод для изменения видимости элементов управления
     * в зависомсти от наличия авторизации пользователя
     *
     * @param authorized true - пользователь авторизован
     *                   false - пользователь не авторизован
     */
    private void setAuthorized(boolean authorized) {
        msgPanel.setVisible(authorized);
        msgPanel.setManaged(authorized);
        authPanel.setVisible(!authorized);
        authPanel.setManaged(!authorized);
        clientsListView.setVisible(authorized);
        clientsListView.setManaged(authorized);
    }

    /**
     * метод для отправки сообщения на сервер
     */
    public void sendMessage() {
        try {
            out.writeUTF(textMessageArea.getText());
            textMessageArea.clear();
            textMessageArea.requestFocus();
        } catch (IOException e) {
            showErrorStringMessage("Server has been down...");
            e.printStackTrace();
        }
    }

    /**
     * метод запроса к серверу для закрытия подключения
     */
    public void sendCloseRequest() {
        try {
            if (out != null && !socket.isClosed() && socket != null) {
                out.writeUTF("/exit");
            }
        } catch (IOException e) {
            showErrorStringMessage("Server has been down...");
            e.printStackTrace();
        }
    }

    /**
     * метод для отправки сообщения по нажатию Enter
     */
    public void sendMessageWithArea(KeyEvent keyEvent) {
        if (keyEvent.getCode() == KeyCode.ENTER) {
            if (!keyEvent.isShiftDown()) {
                if (!textMessageArea.getText().isBlank()) {
                    sendMessage();
                    keyEvent.consume();
                }
            } else {
                textMessageArea.appendText("\n");
                keyEvent.consume();
            }
        }
    }

    /**
     * метод попытки авторизации
     */
    public void tryAuth() {
        connect();
        try {
            out.writeUTF("/auth " + usernameField.getText());
            usernameField.clear();
            textMessageArea.requestFocus();
        } catch (IOException e) {
            showErrorStringMessage("Unable to send authorization request");
        }
    }

    /**
     * метод для подключения к серверу
     */
    private void connect() {
        if (socket != null && !socket.isClosed()) {
            return;
        }
        try {
            socket = new Socket("localhost", 8189);
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
            new Thread(() -> mainClientLogic()).start();
        } catch (IOException e) {
            showErrorStringMessage("Невозможно подключиться к серверу");
        }
    }

    /**
     * жизненный цикл общения с сервером
     */
    private void mainClientLogic() {
        try {
            // принятие сообщений от сервера об авторизации/выходе
            while (true) {
                String message = in.readUTF();
                if (message.equals("/exit")) {
                    closeConnection();
                }
                if (message.startsWith("/authok ")) {
                    username = message.split("\\s+")[1];
                    setAuthorized(true);
                    break;
                }
                chatArea.appendText(message + "\n");
            }
            // принятие сообщений от сервера
            while (true) {
                String message = in.readUTF();
                if (message.startsWith("/")) {
                    if (message.equals("/exit")) {
                        break;
                    }
                    if (message.startsWith("/clients_list ")) {
                        Platform.runLater(() -> {
                            String[] tokens = message.split("\\s+");
                            clientsListView.getItems().clear();
                            for (int i = 1; i < tokens.length; i++) {
                                if (tokens[i].equals(username)) {
                                    clientsListView.getItems().add(tokens[i] + " (You)");
                                    continue;
                                }
                                clientsListView.getItems().add(tokens[i]);
                            }
                        });
                    }
                    continue;
                }
                chatArea.appendText(message + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            closeConnection();
        }
    }

    /**
     * метод закрытия соединения с сервером (сокет, вход. выход. потоков)
     */
    private void closeConnection() {
        setAuthorized(false);
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

    /**
     * метод вывода сообщения об ошибке пользователю
     *
     * @param message сообщение отображаемое пользователю
     */
    private void showErrorStringMessage(String message) {
        new Alert(Alert.AlertType.ERROR, message, ButtonType.OK).showAndWait();
    }

    /**
     * листнер двойного клика по элементу ListView
     */
    public void clientsListDoubleClick(MouseEvent mouseEvent) {
        if (mouseEvent.getClickCount() == 2) {
            String selectedUser = clientsListView.getSelectionModel().getSelectedItem();
            if (selectedUser.contains(" (You)")) {
                return;
            }
            textMessageArea.appendText("/w " + selectedUser + " ");
            textMessageArea.requestFocus();
            textMessageArea.selectEnd();
        }
    }
}
