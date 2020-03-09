package com.geekbrains.geek.cloud.client;

import com.geekbrains.geek.cloud.common.*;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Pair;

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
    private String closeOption;
    private String clientName;
    private URL url;
    private Dialog<Pair<String, String>> regStage;
    private Stage configStage;

    @FXML
    TableView<ServerFile> filesListServer;

    @FXML
    TextField loginField;

    @FXML
    PasswordField passwordField;

    @FXML
    VBox VBoxAuthPanel;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // сохраняю, чтоб можно было использовать повторно
        url = location;
        try {
            String networkConfig = readNetworkConfig();

            boolean networkStarted = Network.start(networkConfig.split(" ", 2)[0], networkConfig.split(" ", 2)[1]);

            if (networkStarted) {
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
                                    // скрываю область ввхода
                                    hideAuthPanel();
                                    // обновляю список файлов
                                    UtilsMainController.refresh(filesListServer, getArrayList((String[]) sm.getMessage()));
                                } else if (sm.getType() == TypesServiceMessages.CLOSE_CONNECTION) {
                                    closeOption = (String) sm.getMessage();
                                    // клиент закрывается - сервер его об этом информирует
                                    System.out.println("Client disconnected from server");
                                    break;
                                } else if (sm.getType() == TypesServiceMessages.ERROR) {
                                    String error = (String) sm.getMessage();
                                    // вывожу пользователю сообщение об ошибке
                                    UtilsMainController.showInformationWindow(error.split("/", 2)[0]);
                                } else if (sm.getType() == TypesServiceMessages.AUTH) {
                                    // если пришел такой ответ, значит аутентификация не удалась, уведомляю об этом пользователя
                                    UtilsMainController.showInformationWindow("Аутентификация не удалась, попробуйте еще раз.");
                                } else if (sm.getType() == TypesServiceMessages.REG) {
                                    // если пришел такой ответ, значит аутентификация не удалась, уведомляю об этом пользователя
                                    UtilsMainController.showInformationWindow("Регистрация не удалась, такой логин уже зарегистрирован.");
                                } else if (sm.getType() == TypesServiceMessages.CLIENTS_NAME) {
                                    clientName = (String) sm.getMessage();
                                    UtilsMainController.setNewTitle(clientName);
                                }
                            }
                        }
                    } catch (ClassNotFoundException | IOException e) {
                        e.printStackTrace();
                    } finally {
                        Network.stop();
                        if (closeOption.equals("close")) {
                            UtilsMainController.closeStage();
                        } else {
                            UtilsMainController.showLogPanel(VBoxAuthPanel, filesListServer);
                        }
                    }
                });
                t.setDaemon(true);
                t.start();

                UtilsMainController.createTableViewSettings(filesListServer);
            } else {
                UtilsMainController.showInformationWindow("Ошибка подключения к серверу");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void hideAuthPanel() {
        if (VBoxAuthPanel.isVisible()) {
            VBoxAuthPanel.setVisible(false);
            VBoxAuthPanel.setManaged(false);
        }
    }

    private List<ServerFile> getArrayList(String[] message) {
        return Arrays.stream(message).map(ServerFile::new).collect(Collectors.toList());
    }

    // Кнопки интерфейса
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

    public void logOut(ActionEvent actionEvent) {
        if (!VBoxAuthPanel.isVisible()) {
            // если панель входа видима, то смысла выпонять логаут нет
            Network.sendMsg(new ServiceMessage(TypesServiceMessages.CLOSE_CONNECTION, "logout"));
            //setNewTitle("");
            UtilsMainController.setNewTitle("");
        }
    }

    public void closeWindow(ActionEvent actionEvent) throws InterruptedException {
        if (Network.isClosed()) {
            // если был выполнен логаут, то соединение закрыто и просто закрываю окно
            UtilsMainController.closeStage();
        } else {
            // отправляю на сервер команду на закрытие. он закроется сам и отправит аналогичную команду клиенту
            Network.sendMsg(new ServiceMessage(TypesServiceMessages.CLOSE_CONNECTION, "close"));
        }
    }

    public void tryToAuth(ActionEvent actionEvent) throws InterruptedException {
        checkConnection();
        Network.sendMsg(new ServiceMessage(TypesServiceMessages.AUTH, loginField.getText() + " " + passwordField.getText().hashCode()));
    }

    public void openRegistrationWindow(ActionEvent actionEvent) throws InterruptedException {
        if (regStage == null) {
            regStage = UtilsMainController.createRegStage();
        }

        Optional<Pair<String, String>> result = regStage.showAndWait();

        if (result.isPresent()) {
            Pair<String, String> loginData = result.get();
            // проверяю активно ли соединение
            checkConnection();
            Network.sendMsg(new ServiceMessage(TypesServiceMessages.REG, loginData.getKey() + " " + loginData.getValue().hashCode()));
        }
    }

    private String readNetworkConfig() throws IOException {
        if (!Files.exists(Paths.get("client/networkSettings.txt"))) {
            Files.createFile(Paths.get("client/networkSettings.txt"));
            Files.write(Paths.get("client/networkSettings.txt"), "localhost 8189".getBytes());
            return "localhost 8189";
        }

        return Files.readAllLines(Paths.get("client/networkSettings.txt")).get(0);
    }

    public void openConfigWindow(ActionEvent actionEvent) throws IOException {
        String settings = Files.readAllLines(Paths.get("client/networkSettings.txt")).get(0);
        configStage = UtilsMainController.createConfigStage(settings.split(" ", 2)[0], settings.split(" ", 2)[1]);
        configStage.show();
    }

    private void checkConnection() throws InterruptedException {
        if (Network.isClosed()) {
            // повторный логин. открываю подключение заново
            initialize(url, null);
            // даю фозможность подключиться к серверу
            Thread.sleep(1000);
        }
    }


    public void openConsole(ActionEvent actionEvent) {
        // для служебного пользования - принимает только одну команду: -1 - выключение сервера
        TextInputDialog dialog = new TextInputDialog();

        dialog.setTitle("Консоль");
        dialog.setHeaderText("Введите команду");

        Optional<String> result = dialog.showAndWait();

        result.ifPresent(value -> {
            if (value.equals("-1")) {
                try {
                    //Network.sendMsg(new ServiceMessage(TypesServiceMessages.CLOSE_CONNECTION, "close"));
                    Network.getOut().writeObject(Integer.parseInt(value));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

    }
}