package ru.netology;

import java.util.List;

public class Request {
    private String verb;
    private String path;
    private List<String> headers;
    private String body;

    public Request(String verb, String path, List<String> headers) {
        this.verb = verb;
        this.path = path;
        this.headers = headers;
    }
    public Request(String verb, String path, List<String> headers, String body) {
        this.verb = verb;
        this.path = path;
        this.headers = headers;
        this.body = body;
    }

    public String getVerb() {
        return verb;
    }

    public String getBody() {
        return body;
    }
    public String getPath() {
        return path;
    }

    public void setBody(String body) {
        this.body = body;
    }

    @Override
    public String toString() {
        return  "Метод: " + verb + "\n"+ "Путь: " + path + "\n" + "Заголовки:\n " + headers
                + "\nТело:\n " + body;
    }
}
