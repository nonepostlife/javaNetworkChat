package ru.postlife.javaChatClient;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

public class Controller implements Initializable {
    @FXML
    TextArea chatArea, textMessageArea;
    @FXML
    Button sendButton;

    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        try {
            Socket socket = new Socket("localhost", 8189);
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());

            Thread thread = new Thread(() -> {
                try {
                    while (true) {
                        String message = in.readUTF();
                        chatArea.appendText(message + "\n");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            thread.start();
        } catch (IOException e) {
            System.out.println("Невозможно подключиться к серверу");
            System.exit(0);
        }
    }

    public void sendMessage() {
        try {
            out.writeUTF(textMessageArea.getText());
            textMessageArea.clear();
        } catch (IOException e) {
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
                textMessageArea.appendText(System.getProperty("line.separator"));
                keyEvent.consume();
            }
        }
    }
}
