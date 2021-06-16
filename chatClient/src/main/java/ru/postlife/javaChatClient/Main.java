package ru.postlife.javaChatClient;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/chat.fxml"));
        Parent root = loader.load();
        Controller controller = loader.getController();
        primaryStage.setTitle("Java Chat Client");
        primaryStage.setScene(new Scene(root, 400, 400));
        primaryStage.setMinHeight(400);
        primaryStage.setMinWidth(400);
        primaryStage.setOnCloseRequest(windowEvent -> {
            controller.disconnect();
            Platform.exit();
            System.exit(0);

        });
        primaryStage.show();

    }

    public static void main(String[] args) {
        launch(args);
    }
}
