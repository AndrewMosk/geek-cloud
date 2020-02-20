package com.geekbrains.geek.cloud.common;

public class ServiceMessage extends AbstractMessage{
    private TypesServiceMessages type;
    private Object[] message;

    public ServiceMessage(TypesServiceMessages type, Object[] message) {
        this.type = type;
        this.message = message;
    }

    public TypesServiceMessages getType() {
        return type;
    }

    public Object getMessage() {
        return message;
    }
}