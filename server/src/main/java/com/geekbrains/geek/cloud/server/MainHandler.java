package com.geekbrains.geek.cloud.server;

import com.geekbrains.geek.cloud.common.FileMessage;
import com.geekbrains.geek.cloud.common.FileRequest;
import com.geekbrains.geek.cloud.common.ServiceMessage;
import com.geekbrains.geek.cloud.common.TypesServiceMessages;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

import java.nio.file.Files;
import java.nio.file.Paths;

public class MainHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("Client connected...");
        // отправляю клиенту список файлов
        StringBuilder stb = new StringBuilder();
        Files.list(Paths.get("server_repository")).map(p -> p.getFileName().toString()).forEach(o -> stb.append(o + "/"));
        stb.delete(stb.length()-1, stb.length());

        ctx.writeAndFlush(new ServiceMessage(TypesServiceMessages.GET_FILES_LIST, stb.toString()));
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        try {
            if (msg instanceof FileRequest) {
                FileRequest fr = (FileRequest) msg;
                if (Files.exists(Paths.get("server_repository/" + fr.getFilename()))) {
                    FileMessage fm = new FileMessage(Paths.get("server_repository/" + fr.getFilename()));
                    ctx.writeAndFlush(fm);
                }
            }
        } finally {
            ReferenceCountUtil.release(msg);
        }
    }



    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
