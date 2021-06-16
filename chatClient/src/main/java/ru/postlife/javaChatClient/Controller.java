package ru.postlife.javaChatClient;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class Controller {
    @FXML
    TextArea chatArea;
    @FXML
    TextArea textMessageArea;
    @FXML
    HBox msgPanel, authPanel;
    @FXML
    TextField usernameField;
    @FXML
    Button sendButton;

    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;

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

    public void tryAuth() {
        connect();
        try {
            if (usernameField.getText().isBlank()) {
                return;
            }
            out.writeUTF("/auth " + usernameField.getText());
            usernameField.clear();
            textMessageArea.requestFocus();
        } catch (IOException e) {
            showErrorStringMessage("Unable to send authorization request");
        }
    }

    public void connect() {
        if (socket != null && !socket.isClosed()) {
            return;
        }
        try {
            socket = new Socket("localhost", 8189);
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());

            Thread readThread = new Thread(() -> {
                try {
                    while (true) {
                        String message = in.readUTF();
                        if (message.equals("/authok")) {
                            msgPanel.setVisible(true);
                            msgPanel.setManaged(true);
                            authPanel.setVisible(false);
                            authPanel.setManaged(false);
                            break;
                        }
                        chatArea.appendText(message + "\n");
                    }
                    while (true) {
                        String message = in.readUTF();
                        chatArea.appendText(message + "\n");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            readThread.start();
        } catch (IOException e) {
            showErrorStringMessage("Невозможно подключиться к серверу");
        }
    }

    public void disconnect() {
        try {
            if (!socket.isClosed() && socket != null)
                socket.close();
        } catch (IOException | NullPointerException e) {
            e.printStackTrace();
        }
    }

    public void showErrorStringMessage(String message) {
        new Alert(Alert.AlertType.ERROR, message, ButtonType.OK).showAndWait();
    }
}
