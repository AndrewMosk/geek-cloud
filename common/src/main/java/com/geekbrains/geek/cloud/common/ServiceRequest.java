package com.geekbrains.geek.cloud.common;

public class ServiceRequest extends AbstractMessage {
    TypesServiceMessages type;

    public ServiceRequest(TypesServiceMessages type) {
        this.type = type;
    }

    public TypesServiceMessages getType() {
        return type;
    }
}
