package ru.netology;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private static final int PORT = 9999;
    private static Server instance;
    private final List<Thread> threads = new ArrayList<>();
    private String path;
    private final ExecutorService threadPool = Executors.newFixedThreadPool(64);
    private final List<String> validPaths = List.of("/index.html", "/spring.svg", "/spring.png", "/resources.html", "/styles.css", "/app.js", "/links.html", "/forms.html", "/classic.html", "/events.html", "/events.js");

    private Server() {
        try (final var serverSocket = new ServerSocket(PORT)) {
            while (true) {
                try (final var socket = serverSocket.accept();
                     final var in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                     final var out = new BufferedOutputStream(socket.getOutputStream());
                ) {
                    String[] parts = processingRequestLine(in);
                    path = parts[1];

                    boolean validationRequestLine = requestValidation(parts);

                    if (!validationRequestLine) {
                        continue;
                    }

                    boolean pageAvailability = noPage(parts, out);

                    if (pageAvailability) {
                        continue;
                    }
                    pageFound(parts, out);

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

        public static Server getInstance() {
            if (instance == null) {
                synchronized (Server.class) {
                    instance = new Server();
                }
            }
            return instance;
        }

        public String[] processingRequestLine (BufferedReader in) throws IOException {
            final String requestLine;
            requestLine = in.readLine();
            final var parts = requestLine.split(" ");
            return parts;
        }

        public boolean requestValidation (String[]parts){
            if (parts.length != 3) {
                return false;
            } else {
                return true;
            }
        }

        public boolean noPage (String[]parts, BufferedOutputStream out) throws IOException {

            if (!validPaths.contains(path)) {
                out.write((
                        "HTTP/1.1 404 Not Found\r\n" +
                                "Content-Length: 0\r\n" +
                                "Connection: close\r\n" +
                                "\r\n"
                ).getBytes());
                out.flush();
                return true;
            } else {
                return false;
            }
        }

        public boolean pageFound (String[]parts, BufferedOutputStream out) throws IOException {
            final var filePath = Path.of(".", "public", path);
            final var mimeType = Files.probeContentType(filePath);
            boolean pageClassic = pageFoundClassic(parts, out, filePath, mimeType);
            if (pageClassic) {
                return false;
            }

            final var length = Files.size(filePath);
            out.write((
                    "HTTP/1.1 200 OK\r\n" +
                            "Content-Type: " + mimeType + "\r\n" +
                            "Content-Length: " + length + "\r\n" +
                            "Connection: close\r\n" +
                            "\r\n"
            ).getBytes());
            Files.copy(filePath, out);
            out.flush();
            return true;
        }

        public boolean pageFoundClassic (String[]parts, BufferedOutputStream out, Path filePath, String mimeType) throws
        IOException {
            // special case for classic
            if (path.equals("/classic.html")) {
                final var template = Files.readString(filePath);
                final var content = template.replace(
                        "{time}",
                        LocalDateTime.now().toString()
                ).getBytes();
                out.write((
                        "HTTP/1.1 200 OK\r\n" +
                                "Content-Type: " + mimeType + "\r\n" +
                                "Content-Length: " + content.length + "\r\n" +
                                "Connection: close\r\n" +
                                "\r\n"
                ).getBytes());
                out.write(content);
                out.flush();
                return true;
            } else {
                return false;
            }
        }
    }