package ru.netology;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.RequestContext;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class Request implements RequestContext {
    private String verb;
    private String path;
    private String protocol;
    private List<String> headers;
    private String body;

    public Request(String verb, String path, String protocol, List<String> headers) {
        this.verb = verb;
        this.path = path;
        this.protocol = protocol;
        this.headers = headers;
    }

    public Request(String verb, String path, String protocol, List<String> headers, String body) {
        this.verb = verb;
        this.path = path;
        this.protocol = protocol;
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
        allPostParam = null;
        return parametersWithSameName;
    }

    public void getPart(String name) {

    }

    public List<FileItem> getParts() throws FileUploadException {
        boolean isMultipart = ServletFileUpload.isMultipartContent(this);
        return new ServletFileUpload(new DiskFileItemFactory()).parseRequest(this);
    }

    public List<NameValuePair> getPostParams() {
        return URLEncodedUtils.parse(body, StandardCharsets.UTF_8, '&');
    }

    @Override
    public String toString() {
        return "Метод: " + verb + "\n" + "Путь: " + path + "\n" + "Заголовки:\n " + headers
                + "\nТело:\n " + body;
    }

    @Override
    public String getCharacterEncoding() {
        return "UTF_8";
    }

    @Override
    public String getContentType() {
        String result = null;
        for (String header : headers)
            if (header.contains("Content-Type")) {
                String[] bufPartsHeader = header.split(":");
                result = bufPartsHeader[1].trim();
            }
        return result;
    }

    @Override
    public int getContentLength() {
        int result = 0;
        for (String header : headers)
            if (header.contains("Content-Length")) {
                String[] bufPartsHeader = header.split(" ");
                result = Integer.parseInt(bufPartsHeader[1]);
            }
        return result;
    }

    @Override
    public InputStream getInputStream() {
        return new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8));
    }
}