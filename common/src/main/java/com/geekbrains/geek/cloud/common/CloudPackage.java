package com.geekbrains.geek.cloud.common;

import java.io.Serializable;

public class CloudPackage implements Serializable {
    private static final long serialVersionUID = -6676682561470229268L;

    private String text;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public CloudPackage(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return String.format("CloudPackage: [%s]", text);
    }
}
