package com.geekbrains.geek.cloud.server;

import com.geekbrains.geek.cloud.common.ServiceMessage;
import com.geekbrains.geek.cloud.common.TypesServiceMessages;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

class AuthHandler extends ChannelInboundHandlerAdapter {
    private boolean authOk = false;
    private String client;
    private static Logger logger;

    public AuthHandler(Logger log) {
        logger = log;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        try {
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

                        try {
                            client = message.substring(0, message.indexOf(" "));

                            authOk = DataBase.authentification(message);
                            processingRegistrationActions(TypesServiceMessages.AUTH, ctx, message, authOk);
                            logger.log(Level.INFO, "AuthHandler authentification client " + client + " " + authOk);
                        } catch (SQLException e) {
                            logger.log(Level.WARNING, e.getMessage());
                            ctx.writeAndFlush(new ServiceMessage(TypesServiceMessages.ERROR, "Ошибка доступа к базе данных/" + e.getMessage()));
                        }

                        break;
                    }
                    case REG: {
                        String message = (String) sm.getMessage();
                        try {
                            client = message.substring(0, message.indexOf(" "));

                            boolean regOk = DataBase.registration(message);
                            processingRegistrationActions(TypesServiceMessages.REG, ctx, message, regOk);
                            logger.log(Level.INFO, "AuthHandler registration client " + client + " " + regOk);
                        } catch (SQLException e) {
                            logger.log(Level.WARNING, e.getMessage());
                            ctx.writeAndFlush(new ServiceMessage(TypesServiceMessages.ERROR, "Ошибка доступа к базе данных/" + e.getMessage()));
                        }

                        break;
                    }
                    case CLOSE_CONNECTION:
                        // пользоватаель решил закрыть окно программы до логина
                        // посылаю команду клиенту на закрытие
                        ctx.writeAndFlush(new ServiceMessage(TypesServiceMessages.CLOSE_CONNECTION, (String) sm.getMessage()));
                        Thread.sleep(1000);
                        // закрываю контекст
                        ctx.close().sync();
                        logger.log(Level.INFO, "AuthHandler client " + client + " disconnected");
                        break;
                }
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, e.getMessage());
            ctx.writeAndFlush(new ServiceMessage(TypesServiceMessages.ERROR, "Неизвестная ошибка работы программы/" + e.getMessage()));
        }
    }

    private void processingRegistrationActions(TypesServiceMessages action, ChannelHandlerContext ctx, String message, boolean result) {
        if (result) {
            authOk = true;
            //  аутентификация пройдена - клиенту должен быть отправлен список его файлов (если регистрация, то действия те же, только список файлов будет пустой)
            ctx.fireChannelRead(new ServiceMessage(TypesServiceMessages.GET_FILES_LIST, client));
        } else {
            // аутентификация (регистрация) неудачна - шлю об этом уведомление клиенту
            ctx.writeAndFlush(new ServiceMessage(action, false));
        }
    }
}