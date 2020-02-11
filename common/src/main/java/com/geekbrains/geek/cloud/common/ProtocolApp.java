package com.geekbrains.geek.cloud.common;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ProtocolApp {
    private String filename;
    private byte[] bytes;
    private long size;

    public String getFilename() {
        return filename;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public long getSize() {
        return size;
    }

    public ProtocolApp(Path path) {
        try {
            this.filename = path.getFileName().toString();
            this.size = Files.size(path);
            this.bytes = generateByteSequence(path.getParent() + "/");
        } catch (IOException e) {
            throw new RuntimeException("Invalid file...");
        }
    }

    public ProtocolApp(byte[] bytes) {
        this.bytes = bytes;
    }

    public void saveFile(String repository) throws IOException {
        parseBytes(repository);
    }

    private void parseBytes(String repository) throws IOException {
        if (bytes[0] == 15) { // сигнальный байт 15 - пришел файл
            // длина имени
            int nameLengthIndex = 4;
            int nameLength = bytes[nameLengthIndex];

            // получаю имя
            byte[] chars = new byte[nameLength];
            int startSignificantBytes = 1 + nameLength + nameLengthIndex;
            for (int i = nameLengthIndex + 1, j = 0; i < startSignificantBytes; i++, j++) {
                chars[j] = bytes[i];
            }
            filename = new String(chars);

            // содержимое файла
            byte[] fileBytes = new byte[bytes.length - startSignificantBytes];
            for (int i = startSignificantBytes, j = 0; i < bytes.length; i++, j++) {
                fileBytes[j] = bytes[i];
            }

            // запись файла
            File file = new File(repository + filename);
            OutputStream outStream = new FileOutputStream(file);
            outStream.write(fileBytes);
        } else { // пришло служебное сообщение

        }
    }

    private byte[] generateByteSequence(String repository) {
        ByteArrayOutputStream bOut = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bOut)) {
            out.write(15);
            int filenameLength = filename.length();
            out.writeInt(filenameLength);
            out.write(filename.getBytes());
            byte[] bytesFromFile = Files.readAllBytes(Paths.get(repository + filename));
            out.write(bytesFromFile);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return bOut.toByteArray();
    }
}
