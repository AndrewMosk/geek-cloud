package com.geekbrains.geek.cloud.common;

public class ServiceMessage extends AbstractMessage{
    private TypesServiceMessages type;
    private String message;

    public ServiceMessage(TypesServiceMessages type, String message) {
        this.type = type;
        this.message = message;
    }

    public TypesServiceMessages getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }
}