package com.geekbrains.geek.cloud.common;

public class FileRequest extends AbstractMessage {
    private String filename;
    private String destinationPath;

    public String getFilename() {
        return filename;
    }

    public String getDestinationPath() {
        return destinationPath;
    }

    public FileRequest(String filename, String destinationPath) {
        this.destinationPath = destinationPath;
        this.filename = filename;
    }
}