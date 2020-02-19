package com.geekbrains.geek.cloud.common;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileMessage extends AbstractMessage {
    private String filename;
    private String destinationPath;
    private byte[] data;

    public String getDestinationPath() {
        return destinationPath;
    }

    public String getFilename() {
        return filename;
    }

    public byte[] getData() {
        return data;
    }

    public FileMessage(Path path, String destinationPath) throws IOException {
        this.destinationPath = destinationPath;
        filename = path.getFileName().toString();
        data = Files.readAllBytes(path);
    }

    public FileMessage(Path path) throws IOException {
        filename = path.getFileName().toString();
        data = Files.readAllBytes(path);
    }
}