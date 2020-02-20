package com.geekbrains.geek.cloud.common;

public class ServerFile {
    private String name;
    private String size;
    private String date;

    public String getName() {
        return name;
    }

    public String getSize() {
        return size;
    }

    public String getDate() {
        return date;
    }

    public ServerFile(String data) {
        String[] tokens = data.split("/", 3);
        this.name = tokens[0];
        this.size = parseSize(tokens[1]);
        this.date = parseDate(tokens[2]);
    }

    private String parseDate(String token) {
        String year = token.split("T", 2)[0];
        String hours = token.substring(token.indexOf("T") + 1, token.indexOf("."));

        return year + " " + hours;
    }

    private String parseSize(String token) {
        long size = Long.parseLong(token);
        size = size/1024;

        return String.valueOf(size);
    }
}