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
            // ретранслирую в MainHandler
            ctx.fireChannelRead(msg);
            return;
        }

        // на утентификацию приходит строка логин и хэш пароля через пробел
        if (msg instanceof ServiceMessage) {
            ServiceMessage sm = (ServiceMessage) msg;
            if (sm.getType() == TypesServiceMessages.AUTH) {
                String message = (String) sm.getMessage();

                authOk = DataBase.authentification(message);
                if (authOk) {
                    //  аутентификация пройдена - клиенту должен быть отправлен список его файлов
                    ctx.fireChannelRead(new ServiceMessage(TypesServiceMessages.GET_FILES_LIST, null));
                } else {
                    // аутентификация неудачна - шлю об этом уведомление клиенту
                    ctx.writeAndFlush(new ServiceMessage(TypesServiceMessages.AUTH, authOk));
                }
            }
        }
    }
}
