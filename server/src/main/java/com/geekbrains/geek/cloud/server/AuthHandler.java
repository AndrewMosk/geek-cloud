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
            switch (sm.getType()) {
                case AUTH: {
                    String message = (String) sm.getMessage();

                    authOk = DataBase.authentification(message);
                    processingRegistrationActions(TypesServiceMessages.AUTH, ctx, message, authOk);
                    break;
                }
                case REG: {
                    String message = (String) sm.getMessage();

                    boolean regOk = DataBase.registration(message);
                    processingRegistrationActions(TypesServiceMessages.REG, ctx, message, regOk);

                    break;
                }
                case CLOSE_CONNECTION:
                    // пользоватаель решил закрыть окно программы до логина
                    // посылаю команду клиенту на закрытие
                    ctx.writeAndFlush(new ServiceMessage(TypesServiceMessages.CLOSE_CONNECTION, (String) sm.getMessage()));
                    Thread.sleep(1000);
                    // закрываю контекст
                    ctx.close().sync();
                    System.out.println("Client disconnected");
                    break;
            }
        }
    }

    private void processingRegistrationActions(TypesServiceMessages action, ChannelHandlerContext ctx, String message, boolean result) {
        if (result) {
            authOk = true;
            //  аутентификация пройдена - клиенту должен быть отправлен список его файлов (если регистрация, то действия те же, только список файлов будет пустой)
            ctx.fireChannelRead(new ServiceMessage(TypesServiceMessages.GET_FILES_LIST, message.substring(0, message.indexOf(" "))));
        } else {
            // аутентификация (регистрация) неудачна - шлю об этом уведомление клиенту
            ctx.writeAndFlush(new ServiceMessage(action, false));
        }
    }
}
