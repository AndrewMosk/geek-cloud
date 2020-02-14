package com.geekbrains.geek.cloud.client;

import com.geekbrains.geek.cloud.common.ProtocolApp;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class Client {
    private static final String REPOSITORY_NAME = "client_repository/";

    public Client() {
        try (Socket socket = new Socket("localhost", 8189)) {
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            String filename = "test file.txt";

            ProtocolApp protocolApp = new ProtocolApp(REPOSITORY_NAME);
            protocolApp.clientSendFile(out, filename);

            System.out.println("Клиент отправил файл " + filename + "\n" + "Клиент отключился");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}