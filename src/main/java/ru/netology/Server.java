package ru.netology;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

public class Server {
    private static Server instance;
    private String[] parts = null;
    private static final int LENGTH = 3;
    private static final int NUMBER_THREADS = 64;
    private Future<Boolean> task;
    private static final ExecutorService THREAD_POOL = Executors.newFixedThreadPool(NUMBER_THREADS);
    private final Map<Handler, String> handlersVerb = new ConcurrentHashMap<>();
    private final Map<Handler, String> handlersPath = new ConcurrentHashMap<>();

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
            handlersVerb.putIfAbsent(handler, verb);
            handlersPath.putIfAbsent(handler, path);
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
                    boolean noEmptyRequest = processingRequestLine(in);

                    if (noEmptyRequest == false) {
                        System.out.println("Прислана пустая строка");
                        continue;
                    }

                    if (!requestValidation(parts)) {
                        continue;
                    }

                    Request request = buildRequest(parts, in);
                    Handler desiredHandler = handlerSearch(request);

                    if (desiredHandler == null) {
                        out.write((
                                "HTTP/1.1 404 Not Found\r\n" +
                                        "Content-Length: 0\r\n" +
                                        "Connection: close\r\n" +
                                        "\r\n"
                        ).getBytes());
                        out.flush();
                        continue;
                    }

                    desiredHandler.handle(request, out);

                } catch (IOException | InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                    // Если ошибка прекратить потоки и цикл
                    THREAD_POOL.shutdown();
                    break;
                }
            }
        }
    }

    public boolean processingRequestLine(BufferedReader in) throws IOException, ExecutionException,
            InterruptedException {
        Callable<Boolean> myCallable = () -> {
            final String requestLine;
            requestLine = in.readLine();
            if (requestLine != null) {
                parts = requestLine.split(" ");
                return true;
            } else {
                return false;
            }
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

    public Request buildRequest(String[] parts, BufferedReader in) throws IOException,
            ExecutionException, InterruptedException {
        Callable<Request> myCallable = () -> {
            String verb = parts[0];
            String path = parts[1];
            List<String> headers = getHeaders(in);
            String body = null;

            for (String result : headers) {
                if (result.contains("Content-Length")) {
                    String[] contLeg = result.split(" ");
                    int length = Integer.parseInt(contLeg[1]);
                    body = getBody(in, length);
                }
            }

            if (body != null) {
                return new Request(verb, path, headers, body);
            } else {
                return new Request(verb, path, headers);
            }
        };
        Future<Request> taskRequest = THREAD_POOL.submit(myCallable);
        return taskRequest.get();
    }

    public Handler handlerSearch(Request request) throws ExecutionException, InterruptedException {
        Callable<Handler> myCallable = () -> {
            Handler desiredHandler = null;
            Handler verbName = null;

            String[] pathFragments = request.getPath().split("\\?");
            //так можно и параметры из Query String получить pathFragments[1] и split
            String onlyPath = pathFragments[0];

            for (Map.Entry<Handler, String> kv : handlersVerb.entrySet()) {
                if (kv.getValue().equals(request.getVerb())) {
                    verbName = kv.getKey();
                }
            }

            for (Map.Entry<Handler, String> kv : handlersPath.entrySet()) {
                if (kv.getKey().equals(verbName) & kv.getValue().equals(onlyPath)) {
                    desiredHandler = kv.getKey();
                }
            }
            return desiredHandler;
        };
        Future<Handler> taskRequest = THREAD_POOL.submit(myCallable);
        return taskRequest.get();
    }

    public static List<String> getHeaders(BufferedReader in) throws ExecutionException,
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

    public static String getBody(BufferedReader in, int length) throws ExecutionException,
            InterruptedException {
        Callable<String> myCallable = () -> {
            StringBuffer builderBuffer = new StringBuffer();
            char[] buffer = new char[length];

            in.read(buffer, 0, length);

            for (int i = 0; i < length; i++) {
                builderBuffer.append(buffer[i]);
            }
            return builderBuffer.toString().trim();
        };
        Future<String> taskList = THREAD_POOL.submit(myCallable);
        return taskList.get();
    }
}