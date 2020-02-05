package com.geekbrains.geek.cloud.common;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

public class TempProtocolApp {
    public static void main(String[] args) {
        ByteArrayOutputStream bOut = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bOut)) {
            out.write(15);
            String filename = "1.txt";
            int filenameLength = filename.length();
            out.writeInt(filenameLength);
            out.write(filename.getBytes());
            byte[] bytesFromFile = Files.readAllBytes(Paths.get("1.txt"));
            out.write(bytesFromFile);

            System.out.println("Bytes: " + Arrays.toString(bOut.toByteArray()));
            System.out.println("String: " + new String(bOut.toByteArray()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
