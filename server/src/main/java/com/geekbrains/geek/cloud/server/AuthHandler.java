package com.geekbrains.geek.cloud.server;

import com.geekbrains.geek.cloud.common.ServiceMessage;
import com.geekbrains.geek.cloud.common.TypesServiceMessages;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.util.logging.Level;
import java.util.logging.Logger;

class AuthHandler extends ChannelInboundHandlerAdapter {
    private boolean authOk = false;
    private String  client;
    private static Logger logger;

    public AuthHandler(Logger log) {
        logger = log;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        logger.log(Level.INFO, "AuthHandler channelActive");
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        logger.log(Level.INFO, "AuthHandler channelRead");
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
                    client = message.substring(0, message.indexOf(" "));

                    authOk = DataBase.authentification(message);
                    processingRegistrationActions(TypesServiceMessages.AUTH, ctx, message, authOk);
                    logger.log(Level.INFO, "AuthHandler authentification client " + client + " " + authOk);

                    break;
                    // В ЦЕЛОМ ЛОГИРОВАНИЕ РАБОТАЕТ
                    // НУЖНО ПОДНАСТРОИТЬ - ГДЕ-ТО ДОБАВИТЬ, А ГДЕ-ТО УБРАТЬ. НУ И НА КЛИЕНТЕ СДЕЛАТЬ
                    // ВАЖНО! ПОСМОТРЕТЬ МЕСТА, ГДЕ НУЖНО УБРАТЬ ПРОБРОС ИСКЛЮЧЕНИЯ В СИГНАТУРУ МЕТОДА,
                    // ЗАМЕНИВ НА TRY CATCH - ЧТОБ В БЛОК CATCH ПРИКРУТИТЬ ЛОГ ОШИБКИ, А НЕ ПРОСТО ИНФО
                }
                case REG: {
                    String message = (String) sm.getMessage();
                    client = message.substring(0, message.indexOf(" "));

                    boolean regOk = DataBase.registration(message);
                    processingRegistrationActions(TypesServiceMessages.REG, ctx, message, regOk);
                    logger.log(Level.INFO, "AuthHandler registration client " + client + " " + regOk);

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
