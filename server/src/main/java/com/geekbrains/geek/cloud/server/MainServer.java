package com.geekbrains.geek.cloud.server;

import com.geekbrains.geek.cloud.common.CloudPackage;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class MainServer {
    public static void main(String[] args) {
        bytesServerExample();
    }

    private static void bytesServerExample() {
        try (ServerSocket serverSocket = new ServerSocket(8189)) {
            System.out.println("Сервер запущен. Ожидаем подключение клиента");
            try (Socket socket = serverSocket.accept();
                 BufferedInputStream in = new BufferedInputStream(socket.getInputStream())) {
                System.out.println("Клиент подключился");
                int n;
                while ((n = in.read()) != -1) {
                    System.out.print((char)n);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void serializableServerExample() {
        try (ServerSocket serverSocket = new ServerSocket(8189)) {
            System.out.println("Сервер запущен. Ожидаем подключение клиента");
            try (Socket socket = serverSocket.accept();
                 ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {
                System.out.println("Клиент подключился");
                CloudPackage cp = (CloudPackage)in.readObject();
                System.out.println("Получен пакет от клиента: " + cp);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
