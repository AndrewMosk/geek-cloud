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
//    @FXML
//    TextField tfFileName;

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
                        FileMessage fm = (FileMessage) am;
                        Files.write(Paths.get("client_storage/" + fm.getFilename()), fm.getData(), StandardOpenOption.CREATE);
                        refresh(null);
                    }

                    if (am instanceof ServiceMessage) {
                        ServiceMessage sm = (ServiceMessage) am;
                        String[] serverFilesList = parseServerFilesList(sm.getMessage());
                        refresh(serverFilesList);
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
        // файлы приходять одной строкой, разделенные пробелом
        return message.split("/");
    }

    public void refresh(String[] serverFilesList) {
        updateUI(() -> {
            try {
                filesListClient.getItems().clear();
                Files.list(Paths.get("client_repository")).map(p -> p.getFileName().toString()).forEach(o -> filesListClient.getItems().add(o));
            } catch (IOException e) {
                e.printStackTrace();
            }

            // список файлов с сервера
            //Network.sendMsg(new ServiceMessage(TypesServiceMessages.GET_FILES_LIST)); // для начала эту спискок своих файлов сервер сам будет отправлять клиенту при старте!!
            // здесь просто обновление
            if (serverFilesList != null) {
                filesListServer.getItems().clear();
                Arrays.stream(serverFilesList).forEach(o -> filesListServer.getItems().add(o));
            }

            // ЭТО НЕ ПРАВИЛЬНО! КЛИЕНТ ДОЛЖЕН ЗАПРАШИВАТЬ У СЕРВЕРА СПИСОК ФАЙЛОВ - К ПАПКЕ У НЕГО НИКАКОГО ДОСТУПА НЕТ!!!
//            try {
//                filesListServer.getItems().clear();
//                Files.list(Paths.get("server_repository")).map(p -> p.getFileName().toString()).forEach(o -> filesListServer.getItems().add(o));
//            } catch (IOException e) {
//                e.printStackTrace();
//            }

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
        // загрузка выделенного файла из клиентского хранилища на сервер
        String filename = filesListClient.getFocusModel().getFocusedItem();
        if (filename != null) {
            System.out.println(filename);
            Network.sendMsg(new FileMessage(Paths.get("client_repository/" + filename)));
        }
    }

    public void pressOnDownloadBtn(ActionEvent actionEvent) {

//        if (tfFileName.getLength() > 0) {
//            Network.sendMsg(new FileRequest(tfFileName.getText()));
//            tfFileName.clear();
//        }
    }
}
