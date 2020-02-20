package com.geekbrains.geek.cloud.server;

import com.geekbrains.geek.cloud.common.*;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.stream.Stream;

public class MainHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("Client connected");
        // отправляю клиенту список файлов
        ctx.writeAndFlush(new ServiceMessage(TypesServiceMessages.GET_FILES_LIST, getFileList()));
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        try {
            if (msg instanceof FileRequest) {
                // отправка файла клиенту
                FileRequest fr = (FileRequest) msg;
                if (Files.exists(Paths.get("server_repository/" + fr.getFilename()))) {
                    FileMessage fm = new FileMessage(Paths.get("server_repository/" + fr.getFilename()), fr.getDestinationPath());
                    ctx.writeAndFlush(fm);
                    System.out.println("File " + fr.getFilename() + " sent to client");
                }
            }

            if (msg instanceof FileMessage) {
                // прием файла от клиента
                FileMessage fm = (FileMessage) msg;
                Files.write(Paths.get("server_repository/" + fm.getFilename()), fm.getData(), StandardOpenOption.CREATE);
                System.out.println("File " + fm.getFilename() + " received from client");

                // отправление на клиент нового списка севреных файлов
                ctx.writeAndFlush(new ServiceMessage(TypesServiceMessages.GET_FILES_LIST, getFileList()));
            }

            if (msg instanceof ServiceMessage) {
                ServiceMessage sm = (ServiceMessage) msg;
                if (sm.getType() == TypesServiceMessages.CLOSE_CONNECTION) {
                    // клиент закрыл соединение
                    // посылаю команду клиенту на закрытие
                    ctx.writeAndFlush(new ServiceMessage(TypesServiceMessages.CLOSE_CONNECTION, null));
                    Thread.sleep(1000);
                    // закрываю контекст
                    ctx.close().sync();
                    System.out.println("Client disconnected");
                }
            }
        } finally {
            ReferenceCountUtil.release(msg);
        }
    }

    private String[] getFileList() throws IOException {
        Stream<Path> pathStream = Files.list(Paths.get("server_repository"));
        ArrayList<String> serverFiles = new ArrayList<>();

        if (pathStream.iterator().hasNext()) {
            Files.list(Paths.get("server_repository")).forEach(file -> {
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