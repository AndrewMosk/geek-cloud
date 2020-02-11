package com.geekbrains.geek.cloud.client;

import com.geekbrains.geek.cloud.common.ProtocolApp;

import java.io.IOException;
import java.net.Socket;
import java.nio.file.Paths;

public class Client {
    private static final String REPOSITORY_NAME = "client_repository/";

    public Client() {
        try (Socket socket = new Socket("localhost", 8189)) {
            System.out.println("Подключение к серверу успешно");
            byte[] dataPackage = new ProtocolApp(Paths.get(REPOSITORY_NAME + "test file.txt")).getBytes();
            socket.getOutputStream().write(dataPackage);
            System.out.println("Файл отправлен");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
