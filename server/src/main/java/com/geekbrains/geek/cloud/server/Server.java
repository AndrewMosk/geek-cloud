package com.geekbrains.geek.cloud.server;

import com.geekbrains.geek.cloud.common.Log;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Server {
    private static Map<MainHandler, String > clients;
    private static Server server;
    private static Logger logger;

    void putClient(MainHandler clientHandler,String  login) {
        clients.put(clientHandler, login);
    }

    void removeClient(MainHandler clientHandler) {
        clients.remove(clientHandler);
    }

    ArrayList<MainHandler> getHandlers(String login) {
        ArrayList<MainHandler> handlersList = new ArrayList<>();
        clients.forEach((k, v) -> {if (v.equals(login)) handlersList.add(k);});

        return handlersList;
    }

    public void run(int port) throws Exception {
        EventLoopGroup mainGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(mainGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            socketChannel.pipeline().addLast(
                                    new ObjectDecoder(50 * 1024 * 1024, ClassResolvers.cacheDisabled(null)),
                                    new ObjectEncoder(),
                                    new AuthHandler(logger),
                                    new MainHandler(server, logger)
                            );
                        }
                    })
                    .childOption(ChannelOption.SO_KEEPALIVE, true);
            ChannelFuture future = b.bind(port).sync();
            logger.log(Level.INFO, "Сервер запущен. Порт " + port);
            future.channel().closeFuture().sync();
        } finally {
            mainGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    public static void main(String[] args) {
        try {
            logger = Log.getLogger();
            clients = new HashMap<>();
            server = new Server();
            server.run(8189);
        } catch (Exception e) {
            logger.log(Level.WARNING, e.getMessage());
        }
    }
}