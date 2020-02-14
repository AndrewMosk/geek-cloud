package com.geekbrains.geek.cloud.common;

import java.io.*;

public class ProtocolApp {
    final int FILE_SIGNAL_BYTE = 15;
    final int SERVICE_MESSAGE_SIGNAL_BYTE = 16;
    String repository;

    public ProtocolApp(String repository) {
        this.repository = repository;
    }

    public void serverReceivePackage(DataInputStream in) throws IOException {
        int signalByte = in.read();
        if (signalByte == FILE_SIGNAL_BYTE) {
            serverSaveFile(in);
        }

    }

    private void serverSaveFile(DataInputStream in) throws IOException {
        short filenameLength = in.readShort();
        byte[] filenameBytes = new byte[filenameLength];
        in.read(filenameBytes);
        String filename = new String(filenameBytes);
        long fileSize = in.readLong();
        try (OutputStream out = new BufferedOutputStream(new FileOutputStream(repository + filename))) {
            for (long i = 0; i < fileSize; i++) {
                out.write(in.read());
            }
        }
    }

    public void clientSendFile(DataOutputStream out, String filename) throws IOException {
        out.write(15);
        short filenameLength = (short) filename.length();
        out.writeShort(filenameLength);
        out.write(filename.getBytes());
        out.writeLong(new File(repository + filename).length());

        byte[] buf = new byte[256];
        try (InputStream in = new FileInputStream(repository + filename)) {
            int n;
            while ((n = in.read(buf)) != -1) {
                out.write(buf, 0, n);
            }
        }
    }

    private void serverReadServiceFile() {

    }

    private void clientSaveFile() {

    }

    private void serverSendFile() {

    }
}