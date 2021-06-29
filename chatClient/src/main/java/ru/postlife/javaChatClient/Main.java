package ru.postlife.javaChatClient;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ListCell;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/chat.fxml"));
        Parent root = loader.load();
        Controller controller = loader.getController();
        primaryStage.setTitle("Java Chat Client");
        primaryStage.setScene(new Scene(root, 600, 400));
        primaryStage.setMinHeight(400);
        primaryStage.setMinWidth(400);
        primaryStage.setOnCloseRequest(windowEvent -> {
            controller.sendCloseRequest();
        });
        primaryStage.show();

    }

    public static void main(String[] args) {
        launch(args);
    }

    /**
     * класс переопределения поведения ячейки ListView
     */
    static class CellFactory extends ListCell<String> {
        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            if (!empty) {
                super.setTextFill(Color.BLACK);
                if (item != null && item.contains(" (You)")) {
                    super.setStyle("-fx-font-weight: bold");
                }
            } else {
                super.setStyle("-fx-font-weight: normal");
            }
            super.setText(item);
        }
    }
}
