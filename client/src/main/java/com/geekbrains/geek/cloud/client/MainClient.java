package com.geekbrains.geek.cloud.client;

import com.geekbrains.geek.cloud.common.CloudPackage;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class MainClient {
    public static void main(String[] args) {
        bytesClientExample();
    }

    private static void bytesClientExample() {
        try (Socket socket = new Socket("localhost", 8189)) {
            byte[] dataPackage = {65, 66, 67, 68, 69};
            socket.getOutputStream().write(dataPackage);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void serializableClientExample() {
        try (Socket socket = new Socket("localhost", 8189);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {
            CloudPackage cp = new CloudPackage("Hello from client");
            out.writeObject(cp);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}