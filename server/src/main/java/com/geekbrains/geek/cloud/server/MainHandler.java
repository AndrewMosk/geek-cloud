package com.geekbrains.geek.cloud.server;

import com.geekbrains.geek.cloud.common.*;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

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
    private Server server;
    private String client;
    private ChannelHandlerContext channelHandlerContext;

    public MainHandler(Server server) {
        this.server = server;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("Client connected");
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
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

            // отправка всем клиентам, выполнившим вход под текущим логином нового списка файлов
            String[] filesList = getFileList();
            server.getHandlers(client).forEach(h -> h.channelHandlerContext.writeAndFlush(new ServiceMessage(TypesServiceMessages.GET_FILES_LIST, filesList)));
        }

        if (msg instanceof ServiceMessage) {
            ServiceMessage sm = (ServiceMessage) msg;
            switch (sm.getType()) {
                case CLOSE_CONNECTION:
                    // клиент закрыл соединение
                    // посылаю команду клиенту на закрытие
                    server.removeClient(this);
                    ctx.writeAndFlush(new ServiceMessage(TypesServiceMessages.CLOSE_CONNECTION, (String) sm.getMessage()));
                    Thread.sleep(1000);
                    // закрываю контекст
                    ctx.close().sync();
                    System.out.println("Client disconnected");
                    break;
                case RENAME_FILE: {
                    String message = (String) sm.getMessage();

                    // имена приходят в строке через >. первое - имя файла, который нужно переименовать, второе - новое имя
                    File file = Paths.get(userRepository + message.split(">", 2)[0]).toFile();
                    boolean success = file.renameTo(Paths.get(userRepository + message.split(">", 2)[1]).toFile());

                    if (success) {
                        // отправка всем клиентам, выполнившим вход под текущим логином нового списка файлов
                        sendFileList();
                    }
                    break;
                }
                case DELETE_FILE: {
                    String message = (String) sm.getMessage();
                    // файлы на удаление в строке через > (символ, который не может фигурировать в имени файла)
                    String[] files = message.split(">");
                    Arrays.stream(files).forEach(f -> Paths.get(userRepository + f).toFile().delete());

                    // отправка всем клиентам, выполнившим вход под текущим логином нового списка файлов
                    sendFileList();

                    break;
                }
                case GET_FILES_LIST:
                    // записываю контекст
                    channelHandlerContext = ctx;

                    // отправка клиенту списка файлов после удачной аутентификации
                    String clientName = (String) sm.getMessage();
                    userRepository = "server_repository/" + clientName + "/";

                    // сюда приходит и после логина и после регистрации. проверяю, создана ли папка пользователя, если нет - создаю
                    if (Files.notExists(Paths.get(userRepository))) {
                        Files.createDirectory(Paths.get(userRepository));
                    }
                    client = clientName;

                    // здесь логин конкретного клиента - всем при этом список файлов обновлять не нужно
                    ctx.writeAndFlush(new ServiceMessage(TypesServiceMessages.GET_FILES_LIST, getFileList()));
                    ctx.writeAndFlush(new ServiceMessage(TypesServiceMessages.CLIENTS_NAME, clientName));

                    // передаю серверу логин и ссылку на хендлер
                    server.putClient(this, clientName);
                    break;
            }
        }
    }

    private void sendFileList() throws IOException {
        String[] filesList = getFileList();
        server.getHandlers(client).forEach(h -> h.channelHandlerContext.writeAndFlush(new ServiceMessage(TypesServiceMessages.GET_FILES_LIST, filesList)));
    }

    private String[] getFileList() throws IOException {
        Stream<Path> pathStream = Files.list(Paths.get(userRepository));
        ArrayList<String> serverFiles = new ArrayList<>();

        if (pathStream.iterator().hasNext()) {
            Files.list(Paths.get(userRepository)).forEach(file -> {
                try {
                    BasicFileAttributes attr = Files.readAttributes(file, BasicFileAttributes.class);
                    serverFiles.add(file.getFileName().toString() + "/" + attr.size() + "/" + attr.lastModifiedTime());

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