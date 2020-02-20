package com.geekbrains.geek.cloud.client;

import com.geekbrains.geek.cloud.common.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.MouseEvent;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.stream.Collectors;

public class MainController implements Initializable {
    final FileChooser fileChooser = new FileChooser();
    final DirectoryChooser directoryChooser = new DirectoryChooser();

    @FXML
    TableView<ServerFile> filesListServer;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Network.start();

        Thread t = new Thread(() -> {
            try {
                while (true) {
                    AbstractMessage am = Network.readObject();
                    if (am instanceof FileMessage) {
                        // прием файла с сервера
                        FileMessage fm = (FileMessage) am;
                        Files.write(Paths.get(fm.getDestinationPath() + "/" + fm.getFilename()), fm.getData(), StandardOpenOption.CREATE);
                        System.out.println("File " + fm.getFilename() + " received from server");
                    }

                    if (am instanceof ServiceMessage) {
                        // прием сервисного сообщения от сервера
                        ServiceMessage sm = (ServiceMessage) am;
                        if (sm.getType() == TypesServiceMessages.GET_FILES_LIST) {
                            // пришел список серверных файлов
                            refresh(getArrayList((String[]) sm.getMessage()));
                        } else if (sm.getType() == TypesServiceMessages.CLOSE_CONNECTION) {
                            // клиент закрывается - сервер его об этом информирует
                            System.out.println("Client disconnected from server");
                            break;
                        }
                    }
                }
            } catch (ClassNotFoundException | IOException e) {
                e.printStackTrace();
            } finally {
                Network.stop();
            }
        });
        t.setDaemon(true);
        t.start();

        createTableViewSettings();
    }

    private List<ServerFile> getArrayList(String[] message) {
        return Arrays.stream(message).map(ServerFile::new).collect(Collectors.toList());
    }

    private void createTableViewSettings() {
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
                    result.ifPresent(name -> Network.sendMsg(new ServiceMessage(TypesServiceMessages.RENAME_FILE, filesListServer.getFocusModel().getFocusedItem().getName() + " " + name)));
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
                    filesListServer.getSelectionModel().getSelectedItems().forEach(f -> files.append(f.getName()).append(" "));
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

    private TextInputDialog createDialogWindow(String defaultValue) {
        // создание диалогового окна с запросом нового имени
        TextInputDialog dialog = new TextInputDialog(defaultValue);
        dialog.setTitle("Rename file");
        dialog.setHeaderText("Enter new filename:");
        dialog.setContentText("Name:");

        return dialog;
    }

    public void refresh(List<ServerFile> serverFilesList) {
        updateUI(() -> {
            // обновление списка файлов с сервера
            filesListServer.getItems().clear();
            filesListServer.setItems(FXCollections.observableArrayList(serverFilesList));
        });
    }

    public static void updateUI(Runnable r) {
        if (Platform.isFxApplicationThread()) {
            r.run();
        } else {
            Platform.runLater(r);
        }
    }

    public void pressOnUploadBtn(ActionEvent actionEvent) {
        // получаю ссылку на основную форму
        Stage primaryStage = MainClient.getPrimaryStage();
        // открываю диалог выбора файлов
        List<File> files = fileChooser.showOpenMultipleDialog(primaryStage);

        if (files != null) {
            files.stream().map(File::getAbsolutePath).forEach(f -> {
                try {
                    Network.sendMsg(new FileMessage(Paths.get(f)));
                    System.out.println("File " + f + " sent to server");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
    }

    private void showInformationWindow (String text) {
        // пока не стал прикручивать
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information message");
        alert.setHeaderText("Message:");
        alert.setContentText(text);

        alert.showAndWait();
    }

    public void pressOnDownloadBtn(ActionEvent actionEvent) {
        // запрос файла с сервера
        if (!filesListServer.getSelectionModel().getSelectedItems().isEmpty()) {
            Stage primaryStage = MainClient.getPrimaryStage();
            // запрашиваю путь - куда скачать файлы
            File directory = directoryChooser.showDialog(primaryStage);

            if (directory != null) {
                filesListServer.getSelectionModel().getSelectedItems().forEach(f -> Network.sendMsg(new FileRequest(f.getName(), directory.getPath())));
            }
        }
    }

    public void tryToAuth(ActionEvent actionEvent) {
    }
}