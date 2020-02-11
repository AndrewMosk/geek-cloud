package com.geekbrains.geek.cloud.server;

import com.geekbrains.geek.cloud.common.ProtocolApp;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    private static final String REPOSITORY_NAME = "server_repository/";

    public Server() {
        try (ServerSocket serverSocket = new ServerSocket(8189)) {
            System.out.println("Сервер запущен. Ожидаем подключение клиента");
            try (Socket socket = serverSocket.accept();
                 BufferedInputStream in = new BufferedInputStream(socket.getInputStream())) {
                System.out.println("Клиент подключился");
                ProtocolApp protocolApp = new ProtocolApp(in.readAllBytes());
                protocolApp.saveFile(REPOSITORY_NAME);
                System.out.println("Файл принят");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
