package com.geekbrains.geek.cloud.common;

public class ServerFile {
    private String name;
    private String size;
    private String date;

    public ServerFile(String data) {
        String[] tokens = data.split("/", 3);
        this.name = tokens[0];
        this.size = tokens[1];
        this.date = tokens[2];
    }
}