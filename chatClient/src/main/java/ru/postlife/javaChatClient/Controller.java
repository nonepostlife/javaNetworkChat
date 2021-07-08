package ru.postlife.javaChatClient;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;

import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Класс управления окном приложения
 */
public class Controller implements Initializable {
    @FXML
    HBox msgPanel, authPanel, changeNicknamePanel, chatPanel;
    @FXML
    VBox registerPanel, mainPanel;
    @FXML
    MenuItem disconnectMenuItem, registerMenuItem;
    @FXML
    CheckMenuItem changeNicknameMenuItem;
    // рабочая область чата
    @FXML
    TextArea chatArea;
    @FXML
    TextField textMessageArea;
    @FXML
    ListView<String> clientsListView;
    // поля ввода, вывода
    @FXML
    TextField usernameField, newNicknameField, loginRegField, nicknameRegField;
    @FXML
    PasswordField passwordField, passwordRegField;
    @FXML
    Label infoLabel;

    private Socket socket;
    private String username;
    private DataInputStream in;
    private DataOutputStream out;
    private BufferedWriter bufferedWriter;
    private File historyFile;
    private boolean isAuthorized;

    /**
     * метод инициализации окна приложения
     */
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        clientsListView.setCellFactory(stringListView -> new Main.CellFactory());
        passwordField.setTextFormatter(new TextFormatter<Object>(change -> {
            if (change.getText().equals(" ")) {
                change.setText("");
            }
            return change;
        }));
        chatArea.textProperty().addListener((observableValue, s, t1) -> chatArea.setScrollTop(Double.MAX_VALUE));
        disconnectMenuItem.setOnAction(event -> {
            sendCloseRequest();
        });
        changeNicknameMenuItem.setOnAction(event -> {
            changeNicknameUiUpdate();
        });
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
            out.writeUTF("/auth " + usernameField.getText() + " " + passwordField.getText());
            textMessageArea.requestFocus();
        } catch (IOException e) {
            showErrorStringMessage("Unable to send authorization request");
        }
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
        changeNicknameMenuItem.setDisable(!changeNicknameMenuItem.isDisable());
        registerMenuItem.setDisable(!registerMenuItem.isDisable());
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
                    return;
                }
                if (message.startsWith("/")) {
                    if (message.startsWith("/register")) {
                        Paint color = message.startsWith("/registerok") ? Color.GREEN : Color.RED;
                        Platform.runLater(() -> {
                            infoLabel.setTextFill(color);
                            infoLabel.setText(message.split("\\s+", 2)[1]);
                        });
                        //sendCloseRequest();
                        continue;
                    }
                    if (message.startsWith("/authok ")) {
                        username = message.split("\\s+")[1];
                        usernameField.clear();
                        passwordField.clear();
                        isAuthorized = true;
                        setAuthorized(isAuthorized);
                        disconnectMenuItem.setDisable(false);
                        historyFile = new File("history_" + username + ".txt");
                        readUserChatHistory();
                        bufferedWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(historyFile, true)));
                        break;
                    }
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
                    if (message.startsWith("/changeok ")) {
                        String[] tokens = message.split("\\s+", 2);
                        username = tokens[1];

                        Path source = Paths.get(historyFile.getName());
                        Path dest = Paths.get("history_" + username + ".txt");
                        bufferedWriter.close();
                        Files.move(source, dest);
                        historyFile = new File("history_" + username + ".txt");
                        bufferedWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(historyFile, true)));

                        Platform.runLater(() -> {
                            changeNicknameMenuItem.setSelected(false);
                            changeNicknameUiUpdate();
                            textMessageArea.requestFocus();
                        });
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
                if (!message.contains("SERVER")) {
                    bufferedWriter.write(message + "\n");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            closeConnectionAndResources();
            disconnectMenuItem.setDisable(true);
        }
    }

    /**
     * метод для чтения истории чата пользователя из файла и добавления последних 100 в зону чата
     */
    private void readUserChatHistory() {
        List<String> chatHistory;
        if (!historyFile.exists()) {
            try {
                historyFile.createNewFile();
                return;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        chatHistory = getHistoryFromFile(historyFile);
        if (chatHistory != null) {
            if (chatHistory.size() > 100) {
                chatHistory = chatHistory.subList(chatHistory.size() - 100, chatHistory.size());
            }
            StringBuilder builder = new StringBuilder();
            chatHistory.forEach(s -> builder.append(s).append("\n"));
            String text = builder.toString();
            chatArea.appendText(text);
            chatArea.setScrollTop(Double.MAX_VALUE);
        }
    }

    /**
     * метод для построчного считывания из файла
     *
     * @param file ссылка на файл
     * @return List<String> - коллекцию строк если файл не пустой, null - если файл пустой
     */
    private List<String> getHistoryFromFile(File file) {
        if (!file.exists()) {
            try {
                historyFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        List<String> history = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)))) {
            String line;
            while ((line = reader.readLine()) != null) {
                history.add(line);
            }
            if (history.size() != 0) {
                return history;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * метод закрытия соединения с сервером (сокет, вход. выход. потоков)
     */
    private void closeConnectionAndResources() {
        isAuthorized = false;
        setAuthorized(isAuthorized);
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
        try {
            if (bufferedWriter != null) {
                bufferedWriter.close();
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
            textMessageArea.clear();
            textMessageArea.appendText("/w " + selectedUser + " ");
            textMessageArea.requestFocus();
            textMessageArea.selectEnd();
        }
    }

    /**
     * метод для отправки запроса смены ника на сервер
     */
    public void changeNickname() {
        try {
            if (out != null && !socket.isClosed() && socket != null) {
                out.writeUTF("/changenickname " + username + " " + newNicknameField.getText());
            }
        } catch (IOException e) {
            showErrorStringMessage("Server has been down...");
            e.printStackTrace();
        }
    }

    /**
     * метод обновления окна приложения при вкл/выкл функции смени никнейма
     */
    private void changeNicknameUiUpdate() {
        changeNicknamePanel.setVisible(!changeNicknamePanel.isVisible());
        changeNicknamePanel.setManaged(!changeNicknamePanel.isManaged());
        newNicknameField.clear();
        newNicknameField.requestFocus();
    }

    public void tryRegister(ActionEvent event) {
        connect();
        try {
            out.writeUTF("/register " + loginRegField.getText() + " " + passwordRegField.getText() + " " + nicknameRegField.getText());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void registrationUiUpdate() {
        registerPanel.setVisible(!registerPanel.isVisible());
        registerPanel.setManaged(!registerPanel.isManaged());
        mainPanel.setVisible(!mainPanel.isVisible());
        mainPanel.setManaged(!mainPanel.isManaged());

        loginRegField.clear();
        passwordRegField.clear();
        nicknameRegField.clear();
        infoLabel.setText("");
    }
}
