package com.geekbrains.geek.cloud.client;

import com.geekbrains.geek.cloud.common.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
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
                            // если он пришел первый раз (после успешной аутентификации), то скрываю область ввхода
                            if (VBoxAuthPanel.isVisible()) {
                                VBoxAuthPanel.setVisible(false);
                                VBoxAuthPanel.setManaged(false);
                            }

                            refresh(getArrayList((String[]) sm.getMessage()));
                        } else if (sm.getType() == TypesServiceMessages.CLOSE_CONNECTION) {
                            closeOption = (String) sm.getMessage();
                            // клиент закрывается - сервер его об этом информирует
                            System.out.println("Client disconnected from server");
                            break;
                        } else if (sm.getType() == TypesServiceMessages.AUTH) {
                            // если пришел такой ответ, значит аутентификация не удалась, уведомляю об этом пользователя
                            showInformationWindow("Аутентификация не удалась, попробуйте еще раз.");
                        } else if (sm.getType() == TypesServiceMessages.REG) {
                            // если пришел такой ответ, значит аутентификация не удалась, уведомляю об этом пользователя
                            showInformationWindow("Регистрация не удалась, такой логин уже зарегистрирован.");
                        } else if (sm.getType() == TypesServiceMessages.CLIENTS_NAME) {
                            clientName = (String) sm.getMessage();
                            setNewTitle(clientName);
                        }
                    }
                }
            } catch (ClassNotFoundException | IOException e) {
                e.printStackTrace();
            } finally {
                Network.stop();
                if (closeOption.equals("close")) {
                    closeStage();
                } else {
                    showLogPanel();
                }
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

    // интерфейс

    public static void updateUI(Runnable r) {
        if (Platform.isFxApplicationThread()) {
            r.run();
        } else {
            Platform.runLater(r);
        }
    }

    public void refresh(List<ServerFile> serverFilesList) {
        updateUI(() -> {
            // обновление списка файлов с сервера
            filesListServer.getItems().clear();
            filesListServer.setItems(FXCollections.observableArrayList(serverFilesList));
        });
    }

    private void showLogPanel() {
        updateUI(() -> {
            // обновление списка файлов с сервера
            VBoxAuthPanel.setVisible(true);
            VBoxAuthPanel.setManaged(true);
            filesListServer.getItems().clear();
        });
    }

    private void setNewTitle(String name) {
        updateUI(() -> {
            Stage stage = MainClient.getPrimaryStage();

            if (name.isEmpty()) {
                stage.setTitle("Cloud storage");
                clientName = "";
            } else {
                String newTitle = stage.getTitle() + " " + name;
                clientName = name;
                stage.setTitle(newTitle);
            }
        });
    }

    private void closeStage() {
        updateUI(() -> {
            Stage stage = MainClient.getPrimaryStage();
            stage.close();
        });
    }

    // вспомогательные окна

    private TextInputDialog createDialogWindow(String defaultValue) {
        // создание диалогового окна с запросом нового имени
        TextInputDialog dialog = new TextInputDialog(defaultValue);
        dialog.setTitle("Rename file");
        dialog.setHeaderText("Enter new filename:");
        dialog.setContentText("Name:");

        return dialog;
    }

    private void showInformationWindow(String text) {
        updateUI(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Information message");
            alert.setHeaderText("Message:");
            alert.setContentText(text);

            alert.showAndWait();
        });
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

    public void tryToAuth(ActionEvent actionEvent) throws InterruptedException {
        checkConnection();
        Network.sendMsg(new ServiceMessage(TypesServiceMessages.AUTH, loginField.getText() + " " + passwordField.getText().hashCode()));
    }

    private void checkConnection() throws InterruptedException {
        if (Network.isClosed()) {
            // повторный логин. открываю подключение заново
            initialize(url, null);
            // даю фозможность подключиться к серверу
            Thread.sleep(1000);
        }
    }

    public void logOut(ActionEvent actionEvent) {
        if (!VBoxAuthPanel.isVisible()) {
            // если панель входа видима, то смысла выпонять логаут нет
            Network.sendMsg(new ServiceMessage(TypesServiceMessages.CLOSE_CONNECTION, "logout"));
            setNewTitle("");
        }
    }

    public void closeWindow(ActionEvent actionEvent) throws InterruptedException {
        if (Network.isClosed()) {
            // если был выполнен логаут, то соединение закрыто и просто закрываю окно
            closeStage();
        } else {
            // отправляю на сервер команду на закрытие. он закроется сам и отправит аналогичную команду клиенту
            Network.sendMsg(new ServiceMessage(TypesServiceMessages.CLOSE_CONNECTION, "close"));
        }
    }

    public void openRegistrationWindow(ActionEvent actionEvent) throws InterruptedException {
        if (regStage == null) {
            // создаю и настраиваю диалоговое окно при первом обращении
            regStage = new Dialog<>();
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
        }

        Optional<Pair<String, String>> result = regStage.showAndWait();

        if (result.isPresent()) {
            Pair<String, String> loginData = result.get();
            //System.out.println(loginData.getKey() + " " + loginData.getValue().hashCode());
            // проверяю активно ли соединение
            checkConnection();
            Network.sendMsg(new ServiceMessage(TypesServiceMessages.REG, loginData.getKey() + " " + loginData.getValue().hashCode()));
        }
    }
}