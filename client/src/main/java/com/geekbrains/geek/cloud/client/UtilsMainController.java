package com.geekbrains.geek.cloud.client;

import com.geekbrains.geek.cloud.common.ServerFile;
import com.geekbrains.geek.cloud.common.ServiceMessage;
import com.geekbrains.geek.cloud.common.TypesServiceMessages;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Pair;

import java.util.List;
import java.util.Optional;

class UtilsMainController {

    static void updateUI(Runnable r) {
        if (Platform.isFxApplicationThread()) {
            r.run();
        } else {
            Platform.runLater(r);
        }
    }

    static void setNewTitle(String name) {
        updateUI(() -> {
            Stage stage = MainClient.getPrimaryStage();

            if (name.isEmpty()) {
                stage.setTitle("Cloud storage");
            } else {
                String newTitle = stage.getTitle() + " " + name;
                stage.setTitle(newTitle);
            }
        });
    }

    static void createTableViewSettings(TableView<ServerFile> filesListServer) {
        // создание контекстного меню
        ContextMenu contextMenu = new ContextMenu();

        MenuItem rename = new MenuItem("Rename");
        rename.setOnAction(new EventHandler<ActionEvent>() {

            @Override
            public void handle(ActionEvent event) {
                // переимновывать разрешаю файлы только по одному :-)
                if (filesListServer.getSelectionModel().getSelectedItems().size() == 1) {
                    // создаю здесь, чтоб задать дефолтное значение
                    TextInputDialog dialog = createDialogWindow(filesListServer.getFocusModel().getFocusedItem().getName());
                    // открываю диалоговое окно
                    Optional<String> result = dialog.showAndWait();
                    result.ifPresent(name -> Network.sendMsg(new ServiceMessage(TypesServiceMessages.RENAME_FILE, filesListServer.getFocusModel().getFocusedItem().getName() + ">" + name)));
                } else {
                    if (contextMenu.isShowing()) {
                        contextMenu.hide();
                    }
                }
            }
        });
        MenuItem delete = new MenuItem("Delete");
        delete.setOnAction(new EventHandler<ActionEvent>() {

            @Override
            public void handle(ActionEvent event) {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Delete File");
                alert.setHeaderText("Are you sure want to delete file(s)? Once deleted, it cannot be restored.");

                Optional<ButtonType> option = alert.showAndWait();
                if (option.get() == ButtonType.OK) {
                    // если пользователь подтверждает удаление файла(ов) - отправляю запрос на удаление
                    StringBuilder files = new StringBuilder();
                    filesListServer.getSelectionModel().getSelectedItems().forEach(f -> files.append(f.getName()).append(">"));
                    files.delete(files.length() - 1, files.length());
                    Network.sendMsg(new ServiceMessage(TypesServiceMessages.DELETE_FILE, files.toString()));
                }
            }
        });

        contextMenu.getItems().addAll(rename, delete);
        // разрешаю множественный выбор
        filesListServer.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        // задаю несколько действий на левый клик - скрываю контекстное меню и снимаю выделение, если клинули на пустое место
        filesListServer.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                if (contextMenu.isShowing()) {
                    contextMenu.hide();
                }
                // сбрасываю выделение, при нажатии мышью на пустое место списка
                if (event.getTarget().toString().contains("'null'")) {
                    filesListServer.getSelectionModel().clearSelection();
                }
            }
        });
        // вызов контекстного меню
        filesListServer.setOnContextMenuRequested(new EventHandler<ContextMenuEvent>() {

            @Override
            public void handle(ContextMenuEvent event) {
                if (filesListServer.getFocusModel().getFocusedItem() != null) {
                    contextMenu.show(filesListServer, event.getScreenX(), event.getScreenY());
                } else {
                    contextMenu.hide();
                }
            }
        });
    }

    static TextInputDialog createDialogWindow(String defaultValue) {
        // создание диалогового окна с запросом нового имени
        TextInputDialog dialog = new TextInputDialog(defaultValue);
        dialog.setTitle("Rename file");
        dialog.setHeaderText("Enter new filename:");
        dialog.setContentText("Name:");

        return dialog;
    }


    static Dialog<Pair<String, String>> createRegStage() {
        // создаю и настраиваю диалоговое окно при первом обращении
        Dialog<Pair<String, String>> regStage = new Dialog<>();
        Stage stage = (Stage) regStage.getDialogPane().getScene().getWindow();
        stage.getIcons().add(new Image("file:client/images/reg.jpg"));
        regStage.setTitle("Регистрация");

        ButtonType loginButtonType = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        regStage.getDialogPane().getButtonTypes().addAll(loginButtonType, ButtonType.CANCEL);

        GridPane gridPane = new GridPane();
        gridPane.setHgap(10);
        gridPane.setVgap(10);
        gridPane.setPadding(new Insets(20, 150, 10, 10));

        TextField login = new TextField();
        login.setPromptText("логин...");
        PasswordField pass = new PasswordField();
        pass.setPromptText("пароль...");

        gridPane.add(new Label("Логин:"), 0, 0);
        gridPane.add(login, 1, 0);
        gridPane.add(new Label("Пароль:"), 2, 0);
        gridPane.add(pass, 3, 0);
        regStage.getDialogPane().setContent(gridPane);

        regStage.setResultConverter(dialogButton -> {
            if (dialogButton == loginButtonType) {
                return new Pair<>(login.getText(), pass.getText());
            } else {
                // стираю введенные данные, если пользователь прервал регистрацию
                login.setText("");
                pass.setText("");
            }
            return null;
        });

        // проверка регистрационных данных
        regStage.setOnCloseRequest(new EventHandler<DialogEvent>() {
            @Override
            public void handle(DialogEvent event) {
                Pair<String, String> pair = regStage.getResult();
                if (pair != null) {
                    if (pair.getKey().isEmpty() || pair.getValue().isEmpty()) {
                        showInformationWindow("Логин и(или) пароль не могут быть пустыми");
                        event.consume();
                    } else if (pair.getKey().contains(" ")) {
                        showInformationWindow("В логине пробелы недопустимы");
                        event.consume();
                    } else {
                        // стираю прошлые данные регистрации
                        login.setText("");
                        pass.setText("");
                    }
                }
            }
        });
        return regStage;
    }

    static void showInformationWindow(String text) {
        updateUI(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Information message");
            alert.setHeaderText("Message:");
            alert.setContentText(text);

            alert.showAndWait();
        });
    }

    static void showLogPanel(VBox VBoxAuthPanel, TableView<ServerFile> filesListServer) {
        UtilsMainController.updateUI(() -> {
            // обновление списка файлов с сервера
            VBoxAuthPanel.setVisible(true);
            VBoxAuthPanel.setManaged(true);
            filesListServer.getItems().clear();
        });
    }

    static void closeStage() {
        updateUI(() -> {
            Stage stage = MainClient.getPrimaryStage();
            stage.close();
        });
    }
    static void refresh(TableView<ServerFile> filesListServer, List<ServerFile> serverFilesList) {
        updateUI(() -> {
            // обновление списка файлов с сервера
            filesListServer.getItems().clear();
            filesListServer.setItems(FXCollections.observableArrayList(serverFilesList));
        });
    }
}
