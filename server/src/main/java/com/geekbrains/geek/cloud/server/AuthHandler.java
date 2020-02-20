package com.geekbrains.geek.cloud.server;

import com.geekbrains.geek.cloud.common.ServiceMessage;
import com.geekbrains.geek.cloud.common.TypesServiceMessages;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

class AuthHandler extends ChannelInboundHandlerAdapter {
    private boolean authOk = false;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (authOk) {
            ctx.fireChannelRead(msg);
            return;
        }

        String input = (String) msg;
        // /auth user1
        if (msg instanceof ServiceMessage) {
            ServiceMessage sm = (ServiceMessage) msg;
            if (sm.getType() == TypesServiceMessages.AUTH) {
                String message = (String) sm.getMessage();

                authOk = tryToAuth(message.split(" ",2)[0], message.split(" ",2)[1]);
                ctx.pipeline().addLast(new MainHandler());
            }
        }


        if (input.split(" ")[0].equals("/auth")) {
            String username = input.split(" ")[1];
            authOk = true;
            ctx.pipeline().addLast(new MainHandler());
        }
    }

    private boolean tryToAuth(String login, String passwordHash) {

        return true;
    }
}
