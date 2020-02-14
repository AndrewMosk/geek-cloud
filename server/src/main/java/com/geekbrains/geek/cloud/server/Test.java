package com.geekbrains.geek.cloud.server;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

public class Test {
    public static void main(String[] args) throws IOException {
        StringBuilder st = new StringBuilder();
        Files.list(Paths.get("server_repository")).map(p -> p.getFileName().toString()).forEach(o -> st.append(o + "/"));
        st.delete(st.length()-1, st.length());

        String[] sss = st.toString().split("/");


        System.out.println(st.toString());
    }
}
