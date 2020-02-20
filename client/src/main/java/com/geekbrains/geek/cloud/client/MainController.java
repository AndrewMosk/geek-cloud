package com.geekbrains.geek.cloud.client;

import com.geekbrains.geek.cloud.common.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
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
                        // прием сервисного сообщения от клиента
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

        createListViewSettings();
    }

    private List<ServerFile> getArrayList(String[] message) {
        return Arrays.stream(message).map(ServerFile::new).collect(Collectors.toList());
    }

    private void createListViewSettings() {
        ContextMenu contextMenu = new ContextMenu();

        MenuItem rename = new MenuItem("Rename");
        rename.setOnAction(new EventHandler<ActionEvent>() {

            @Override
            public void handle(ActionEvent event) {
                //label.setText("Select Menu Item 1");
            }
        });
        MenuItem delete = new MenuItem("Delete");
        delete.setOnAction(new EventHandler<ActionEvent>() {

            @Override
            public void handle(ActionEvent event) {
                //label.setText("Select Menu Item 2");
            }
        });

        contextMenu.getItems().addAll(rename, delete);
        filesListServer.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        filesListServer.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                if (contextMenu.isShowing()) {
                    contextMenu.hide();
                }

                // сбрасываю выделение, при нажатии мышью на пустое место списка
                if (event.getTarget().toString().contains("null")) {
                    filesListServer.getSelectionModel().clearSelection();
                }
            }
        });

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
        Stage primaryStage = MainClient.getPrimaryStage();
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


    public void pressOnDownloadBtn(ActionEvent actionEvent) {
        // запрос файла с сервера
        if (!filesListServer.getSelectionModel().getSelectedItems().isEmpty()) {
            Stage primaryStage = MainClient.getPrimaryStage();
            File directory = directoryChooser.showDialog(primaryStage);

            if (directory != null) {
                //filesListServer.getSelectionModel().getSelectedItems().forEach(f -> Network.sendMsg(new FileRequest(f, directory.getPath())));
            }

        }
    }
}