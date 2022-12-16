package ru.netology;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

public class Server {
    private static Server instance;
    private String path;
    private String[] parts = null;
    private static final int LENGTH = 3;
    private static final int NUMBER_THREADS = 64;
    private Future<Boolean> task;
    private static final ExecutorService THREAD_POOL = Executors.newFixedThreadPool(NUMBER_THREADS);
    private final List<String> VALID_PATHS = List.of("/index.html", "/spring.svg", "/spring.png",
            "/resources.html", "/styles.css", "/app.js", "/links.html", "/forms.html", "/classic.html",
            "/events.html", "/events.js");
    private final Map<String, Handler> handlersVerb = new ConcurrentHashMap<>();
    private final Map<String, Handler> handlersPath = new ConcurrentHashMap<>();

    private Server() {
    }

    public static Server getInstance() {
        if (instance == null) {
            synchronized (Server.class) {
                instance = new Server();
            }
        }
        return instance;
    }

    public void addHandler(String verb, String path, Handler handler) {
        Runnable myRunnable = () -> {
            handlersVerb.putIfAbsent(verb, handler);
            handlersPath.putIfAbsent(path, handler);
        };
        THREAD_POOL.submit(myRunnable);
    }

    public void listen(int port) throws IOException {
        try (final var serverSocket = new ServerSocket(port)) {
            while (true) {
                try (final var socket = serverSocket.accept();
                     final var in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                     final var out = new BufferedOutputStream(socket.getOutputStream());
                ) {
                    processingRequestLine(in);

                    if (!requestValidation(parts)) {
                        continue;
                    }

                    buildRequest(parts, in);

                } catch (IOException | InterruptedException | ExecutionException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public boolean processingRequestLine(BufferedReader in) throws IOException, ExecutionException,
            InterruptedException {
        Callable<Boolean> myCallable = () -> {
            final String requestLine;
            requestLine = in.readLine();
            parts = requestLine.split(" ");
            path = parts[1];
            return true;
        };
        task = THREAD_POOL.submit(myCallable);
        return task.get();
    }

    public boolean requestValidation(String[] parts) throws ExecutionException,
            InterruptedException {
        Callable<Boolean> myCallable = () -> parts.length == LENGTH;
        task = THREAD_POOL.submit(myCallable);
        return task.get();
    }

    public boolean noPage(BufferedOutputStream out) throws IOException,
            ExecutionException, InterruptedException {
        Callable<Boolean> myCallable = () -> {
            if (!VALID_PATHS.contains(path)) {
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
        };
        task = THREAD_POOL.submit(myCallable);
        return task.get();
    }

    public boolean pageFound(BufferedOutputStream out) throws IOException,
            ExecutionException, InterruptedException {
        Callable<Boolean> myCallable = () -> {
            final var filePath = Path.of(".", "public", path);
            final var mimeType = Files.probeContentType(filePath);
            boolean pageClassic = pageFoundClassic(out, filePath, mimeType);
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
        };
        task = THREAD_POOL.submit(myCallable);
        return task.get();
    }

    public boolean pageFoundClassic(BufferedOutputStream out, Path filePath, String mimeType)
            throws ExecutionException, InterruptedException {
        Callable<Boolean> myCallable = () -> {
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
        };
        task = THREAD_POOL.submit(myCallable);
        return task.get();
    }
    public static List<String> getHeaders(BufferedReader in) throws IOException, ExecutionException,
            InterruptedException {
        Callable<List<String>> myCallable = () -> {
            List<String> headers = new ArrayList<>();
            while (true) {
                String header = in.readLine();
                if (!header.equals("")) {
                    headers.add(header);
                } else {
                    break;
                }
            }
            return headers;
        };
        Future<List<String>> taskList = THREAD_POOL.submit(myCallable);
        return taskList.get();
    }
    public static String getBody(BufferedReader in) throws ExecutionException, InterruptedException {
        Callable<String> myCallable = () -> {
            String body = in.readLine();
            return body;
        };
        Future<String> taskBody = THREAD_POOL.submit(myCallable);
        return taskBody.get();
    }

    public Request buildRequest(String[] parts, BufferedReader in) throws IOException,
            ExecutionException, InterruptedException {
        Callable<Request> myCallable = () -> {
            String verb = parts[0];
            List<String> headers = getHeaders(in);
            String body = null;

            for (String result : headers) {
                if (result.contains("Content-Length")) {
                    body = getBody(in);
                }
            }

            if (body != null) {
                return new Request(verb, headers, body);
            } else {
                return new Request(verb, headers);
            }
        };
        Future<Request> taskRequest = THREAD_POOL.submit(myCallable);
        return taskRequest.get();
    }
}