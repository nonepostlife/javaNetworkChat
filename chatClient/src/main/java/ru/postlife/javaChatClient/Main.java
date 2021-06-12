package ru.postlife.javaChatClient;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{
        Parent root = FXMLLoader.load(getClass().getResource("/chat.fxml"));
        primaryStage.setTitle("Java Chat Client");
        primaryStage.setScene(new Scene(root, 400, 400));
        primaryStage.setMinHeight(400);
        primaryStage.setMinWidth(400);
        primaryStage.show();
    }


    public static void main(String[] args) {
        launch(args);
    }
}
