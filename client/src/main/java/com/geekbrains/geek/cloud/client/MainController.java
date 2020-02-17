package com.geekbrains.geek.cloud.client;

import com.geekbrains.geek.cloud.common.*;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.ResourceBundle;

public class MainController implements Initializable {

    @FXML
    ListView<String> filesListClient;

    @FXML
    ListView<String> filesListServer;

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
                        Files.write(Paths.get("client_repository/" + fm.getFilename()), fm.getData(), StandardOpenOption.CREATE);
                        System.out.println("File " + fm.getFilename() + " received from server");
                        refresh(null);
                    }

                    if (am instanceof ServiceMessage) {
                        // прием сервисного сообщения от клиента
                        ServiceMessage sm = (ServiceMessage) am;
                        if (sm.getType() == TypesServiceMessages.GET_FILES_LIST) {
                            // пришел список серверных файлов
                            String[] serverFilesList = parseServerFilesList(sm.getMessage());
                            refresh(serverFilesList);
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
        refresh(null);
    }

    private String[] parseServerFilesList(String message) {
        // файлы приходят одной строкой, разделенные /
        return message.split("/");
    }

    public void refresh(String[] serverFilesList) {
        updateUI(() -> {
            try {
                // обновление списка файлов клиента
                filesListClient.getItems().clear();
                Files.list(Paths.get("client_repository")).map(p -> p.getFileName().toString()).forEach(o -> filesListClient.getItems().add(o));
            } catch (IOException e) {
                e.printStackTrace();
            }

            // обновление списка файлов с сервера
            if (serverFilesList != null) {
                filesListServer.getItems().clear();
                Arrays.stream(serverFilesList).forEach(o -> filesListServer.getItems().add(o));
            }
        });
    }

    public static void updateUI(Runnable r) {
        if (Platform.isFxApplicationThread()) {
            r.run();
        } else {
            Platform.runLater(r);
        }
    }

    public void pressOnUploadBtn(ActionEvent actionEvent) throws IOException {
        // отправка выделенного файла из клиентского хранилища на сервер
        String filename = filesListClient.getFocusModel().getFocusedItem();
        if (filename != null) {
            Network.sendMsg(new FileMessage(Paths.get("client_repository/" + filename)));
            System.out.println("File " + filename + " sent to server");
        }
    }

    public void pressOnDownloadBtn(ActionEvent actionEvent) {
        // запрос файла с сервера
        String filename = filesListServer.getFocusModel().getFocusedItem();
        if (filename != null) {
            Network.sendMsg(new FileRequest(filename));
        }
    }
}