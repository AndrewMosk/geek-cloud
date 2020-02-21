package com.geekbrains.geek.cloud.server;

import com.geekbrains.geek.cloud.common.*;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Stream;

public class MainHandler extends ChannelInboundHandlerAdapter {
    private String userRepository;

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("Client connected");
        // отправляю клиенту список файлов
        //ctx.writeAndFlush(new ServiceMessage(TypesServiceMessages.GET_FILES_LIST, getFileList()));
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        try {
            System.out.println(userRepository);
            if (msg instanceof FileRequest) {
                // отправка файла клиенту
                FileRequest fr = (FileRequest) msg;
                if (Files.exists(Paths.get(userRepository + fr.getFilename()))) {
                    FileMessage fm = new FileMessage(Paths.get(userRepository + fr.getFilename()), fr.getDestinationPath());
                    ctx.writeAndFlush(fm);
                    System.out.println("File " + userRepository + fr.getFilename() + " sent to client");
                }
            }

            if (msg instanceof FileMessage) {
                // прием файла от клиента
                FileMessage fm = (FileMessage) msg;
                Files.write(Paths.get(userRepository + fm.getFilename()), fm.getData(), StandardOpenOption.CREATE);
                System.out.println("File " + userRepository + fm.getFilename() + " received from client");

                // отправление на клиент нового списка севреных файлов
                ctx.writeAndFlush(new ServiceMessage(TypesServiceMessages.GET_FILES_LIST, getFileList()));
            }

            if (msg instanceof ServiceMessage) {
                ServiceMessage sm = (ServiceMessage) msg;
                if (sm.getType() == TypesServiceMessages.CLOSE_CONNECTION) {
                    // клиент закрыл соединение
                    // посылаю команду клиенту на закрытие
                    ctx.writeAndFlush(new ServiceMessage(TypesServiceMessages.CLOSE_CONNECTION, (String) sm.getMessage()));
                    Thread.sleep(1000);
                    // закрываю контекст
                    ctx.close().sync();
                    System.out.println("Client disconnected");
                } else if (sm.getType() == TypesServiceMessages.RENAME_FILE) {
                    String message = (String) sm.getMessage();

                    // имена приходят в строке через пробел. первое - имя файла, который нужно переименовать, второе - новое имя
                    File file = Paths.get(userRepository + message.split(" ", 2)[0]).toFile();
                    boolean success = file.renameTo(Paths.get(userRepository + message.split(" ", 2)[1]).toFile());

                    if (success) {
                        ctx.writeAndFlush(new ServiceMessage(TypesServiceMessages.GET_FILES_LIST, getFileList()));
                    }
                } else if (sm.getType() == TypesServiceMessages.DELETE_FILE) {
                    String message = (String) sm.getMessage();
                    // файлы на удаление в строке через пробел
                    String[] files = message.split(" ");
                    Arrays.stream(files).forEach(f -> Paths.get(userRepository + f).toFile().delete());
                    // новый список файлов
                    ctx.writeAndFlush(new ServiceMessage(TypesServiceMessages.GET_FILES_LIST, getFileList()));
                } else if (sm.getType() == TypesServiceMessages.GET_FILES_LIST) {
                    userRepository = "server_repository/" + (String) sm.getMessage()  + "/";
                    System.out.println(userRepository);
                    ctx.writeAndFlush(new ServiceMessage(TypesServiceMessages.GET_FILES_LIST, getFileList()));
                }
            }
        } finally {
            ReferenceCountUtil.release(msg);
        }
    }

    private String[] getFileList() throws IOException {
        Stream<Path> pathStream = Files.list(Paths.get(userRepository));
        ArrayList<String> serverFiles = new ArrayList<>();

        if (pathStream.iterator().hasNext()) {
            Files.list(Paths.get(userRepository)).forEach(file -> {
                try {
                    BasicFileAttributes attr = Files.readAttributes(file, BasicFileAttributes.class);
                    serverFiles.add( file.getFileName().toString() + "/" + attr.size() + "/" + attr.lastModifiedTime());

                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }

        return serverFiles.toArray(new String[0]);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}