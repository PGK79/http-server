package ru.netology;

import java.util.List;

public class Request {
    private String verb;
    private List<String> headers;
    private String body;

    public Request(String verb, List<String> headers) {
        this.verb = verb;
        this.headers = headers;
    }
    public Request(String verb, List<String> headers, String body) {
        this.verb = verb;
        this.headers = headers;
        this.body = body;
    }
}
