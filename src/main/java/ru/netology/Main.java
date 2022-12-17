package ru.netology;

import java.io.BufferedOutputStream;
import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        final var server = Server.getInstance();

        server.addHandler("GET", "/messages", new Handler() {
            public void handle(Request request, BufferedOutputStream responseStream)
                    throws IOException {
                // У GET запроса тела нет, а условия задачи не ограничивают что отправить
                // в теле ответа
                request.setBody("Very important information");
                String content = request.getBody();
                final var mimeType = "text/plain";
                final var length = content.length();
                responseStream.write((
                        "HTTP/1.1 200 OK\r\n" +
                                "Content-Type: " + mimeType + "\r\n" +
                                "Content-Length: " + length + "\r\n" +
                                "Connection: close\r\n" +
                                "\r\n"
                ).getBytes());
                responseStream.write(content.getBytes());
                responseStream.flush();
            }
        });
        server.addHandler("POST", "/messages", new Handler() {
            public void handle(Request request, BufferedOutputStream responseStream) throws IOException {
                request.setBody("Everything came well. Your content will remain with us.");
                String contentPost = request.getBody();
                final var mimeType = "text/plain";
                final var length = contentPost.length();
                responseStream.write((
                        "HTTP/1.1 200 OK\r\n" +
                                "Content-Type: " + mimeType + "\r\n" +
                                "Content-Length: " + length + "\r\n" +
                                "Connection: close\r\n" +
                                "\r\n"
                ).getBytes());
                responseStream.write(contentPost.getBytes());
                responseStream.flush();
            }
        });

        server.listen(9999);
    }
}