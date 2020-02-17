package com.geekbrains.geek.cloud.server;

import com.geekbrains.geek.cloud.common.FileMessage;
import com.geekbrains.geek.cloud.common.FileRequest;
import com.geekbrains.geek.cloud.common.ServiceMessage;
import com.geekbrains.geek.cloud.common.TypesServiceMessages;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
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
                    FileMessage fm = new FileMessage(Paths.get("server_repository/" + fr.getFilename()));
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
                    ctx.writeAndFlush(new ServiceMessage(TypesServiceMessages.CLOSE_CONNECTION, ""));
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

    private String getFileList() throws IOException {
        StringBuilder stb = new StringBuilder();

        // если папка пуста, бросает исключение
        Stream<Path> pathStream = Files.list(Paths.get("server_repository"));
        if (pathStream.iterator().hasNext()) {
            Files.list(Paths.get("server_repository")).map(p -> p.getFileName().toString()).forEach(o -> stb.append(o).append("/"));
            stb.delete(stb.length() - 1, stb.length());
        }

        return stb.toString();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}