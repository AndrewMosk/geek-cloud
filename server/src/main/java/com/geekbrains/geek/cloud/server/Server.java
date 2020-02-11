package com.geekbrains.geek.cloud.server;

import com.geekbrains.geek.cloud.common.ProtocolApp;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    private static final String REPOSITORY_NAME = "server_repository/";

    public Server() {
        try (ServerSocket serverSocket = new ServerSocket(8189)) {
            System.out.println("Сервер запущен");

            try (Socket socket = serverSocket.accept()) {
                System.out.println("Клиент подключился");
                DataInputStream in = new DataInputStream(socket.getInputStream());

                ProtocolApp protocolApp = new ProtocolApp(REPOSITORY_NAME);
                protocolApp.serverReceivePackage(in);
                System.out.println("Файл принят \n" +"Сервер отключен");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}