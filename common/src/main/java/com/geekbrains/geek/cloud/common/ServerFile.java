package com.geekbrains.geek.cloud.common;

import java.nio.file.attribute.FileTime;
import java.util.Date;

public class ServerFile {
    private String name;
    private long size;
    private FileTime date;

    public String getName() {
        return name;
    }

    public long getSize() {
        return size;
    }

    public FileTime getDate() {
        return date;
    }

    public ServerFile(String name, long size, FileTime date) {
        this.name = name;
        this.size = size;
        this.date = date;
    }

}
