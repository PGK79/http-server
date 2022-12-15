package ru.netology;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.*;

public class Server {
    private static Server instance;
    private String path;
    private String[] parts = null;
    private static final int PORT = 9999;
    private static final int LENGTH = 3;
    private static final int NUMBER_THREADS = 64;
    private Future<Boolean> task;
    private final ExecutorService THREAD_POOL = Executors.newFixedThreadPool(NUMBER_THREADS);
    private final List<String> VALID_PATHS = List.of("/index.html", "/spring.svg", "/spring.png",
            "/resources.html", "/styles.css", "/app.js", "/links.html", "/forms.html", "/classic.html",
            "/events.html", "/events.js");

    private Server() {
        try (final var serverSocket = new ServerSocket(PORT)) { //final переменные указанные в коде курса
            // нижним регистром не менял.
            while (true) {
                try (final var socket = serverSocket.accept();
                     final var in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                     final var out = new BufferedOutputStream(socket.getOutputStream());
                ) {
                    processingRequestLine(in);

                    if (!requestValidation(parts) || noPage(out)) {
                        continue;
                    }

                    pageFound(out);

                } catch (IOException e) {
                    e.printStackTrace();
                } catch (ExecutionException | InterruptedException e) {
                    throw new RuntimeException(e);
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
}