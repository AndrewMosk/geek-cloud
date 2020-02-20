package com.geekbrains.geek.cloud.client;

import com.geekbrains.geek.cloud.common.ServiceMessage;
import com.geekbrains.geek.cloud.common.TypesServiceMessages;
import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public class MainClient extends Application {
    private static Stage primaryStage;

    private void setPrimaryStage(Stage stage) {
        MainClient.primaryStage = stage;
    }

    static Stage getPrimaryStage() {
        return MainClient.primaryStage;
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        setPrimaryStage(primaryStage);

        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/main.fxml"));
        Parent root = fxmlLoader.load();
        primaryStage.setTitle("Box Client");
        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        primaryStage.show();

        primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent event) {
                Network.sendMsg(new ServiceMessage(TypesServiceMessages.CLOSE_CONNECTION, null));
            }
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}