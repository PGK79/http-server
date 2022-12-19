package ru.netology;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
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

    public String getPath() {
        return path;
    }

    public NameValuePair getQueryParam(String name) { //поиск именно по ключу
        List<NameValuePair> allQueryParams = getQueryParams();
        NameValuePair queryParam = null;
        for (NameValuePair result : allQueryParams) {
            if (name.equals(result.getName())) {
                queryParam = result;
            }
        }
        return queryParam;
    }

    public List<NameValuePair> getQueryParams() {
        String[] pathAndQuery = path.split("\\?");
        return URLEncodedUtils.parse(pathAndQuery[1], StandardCharsets.UTF_8, '&');
    }

    public List<NameValuePair> getPostParam(String name) {
        List<NameValuePair> allPostParam = getPostParams();
        List<NameValuePair> parametersWithSameName = new ArrayList<>();
        for (NameValuePair result : allPostParam) {
            if (name.equals(result.getName())) {
                parametersWithSameName.add(result);
            }
        }
        return parametersWithSameName;
    }

    public List<NameValuePair> getPostParams() {
        return URLEncodedUtils.parse(body, StandardCharsets.UTF_8, '&');

    }

    @Override
    public String toString() {
        return "Метод: " + verb + "\n" + "Путь: " + path + "\n" + "Заголовки:\n " + headers
                + "\nТело:\n " + body;
    }
}