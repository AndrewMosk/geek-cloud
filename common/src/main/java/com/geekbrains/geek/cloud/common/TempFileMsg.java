package com.geekbrains.geek.cloud.common;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;

public class TempFileMsg implements Serializable {
    private static final long serialVersionUID = 3309158538940021582L;

    private String filename;
    private byte[] bytes;
    private long size;

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public void setBytes(byte[] bytes) {
        this.bytes = bytes;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public TempFileMsg(Path path) {
        try {
            this.filename = path.getFileName().toString();
            this.size = Files.size(path);
            this.bytes = Files.readAllBytes(path);
        } catch (IOException e) {
            throw new RuntimeException("Invalid file...");
        }
    }
}