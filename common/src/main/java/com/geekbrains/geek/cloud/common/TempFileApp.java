package com.geekbrains.geek.cloud.common;

import java.io.File;
import java.io.Serializable;
import java.nio.file.Paths;

public class TempFileApp {
    public static void main(String[] args) {
        TempFileMsg tfm = new TempFileMsg(Paths.get("1.txt"));
        // ...

        File[] files = new File(".idea").listFiles();
        for (File f : files) {
            System.out.println(f.getName());
        }
    }
}
