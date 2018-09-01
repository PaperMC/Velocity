package com.velocitypowered.proxy.network.http;

public class SimpleHttpResponse {
    private final int code;
    private final String body;

    SimpleHttpResponse(int code, String body) {
        this.code = code;
        this.body = body;
    }

    public int getCode() {
        return code;
    }

    public String getBody() {
        return body;
    }

    @Override
    public String toString() {
        return "SimpleHttpResponse{" +
                "code=" + code +
                ", body='" + body + '\'' +
                '}';
    }
}
