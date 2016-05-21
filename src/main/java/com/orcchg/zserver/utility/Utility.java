package com.orcchg.zserver.utility;

import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.entity.ContentLengthStrategy;
import org.apache.http.impl.entity.StrictContentLengthStrategy;
import org.apache.http.impl.io.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class Utility {

    public static final String VERSION = "1.0";
    public static final String CONTENT_LIST_OPEN = "[";
    public static final String CONTENT_LIST_CLOSE = "]";

    public static Pair<HttpRequest, InputStream> getRequestFromConnection(InputStream input)
            throws IOException, HttpException {
        HttpTransportMetricsImpl metrics = new HttpTransportMetricsImpl();
        SessionInputBufferImpl buffer = new SessionInputBufferImpl(metrics, 2048);
        buffer.bind(input);
        DefaultHttpRequestParser parser = new DefaultHttpRequestParser(buffer);
        HttpRequest request = parser.parse();
        InputStream bodyStream = getRequestBodyStream(request, buffer);
        return new Pair<>(request, bodyStream);
    }

    public static Map<String, List<String>> splitQuery(URL url) throws UnsupportedEncodingException {
        final Map<String, List<String>> query_pairs = new LinkedHashMap<>();
        final String[] pairs = url.getQuery().split("&");
        for (String pair : pairs) {
            final int idx = pair.indexOf("=");
            final String key = idx > 0 ? URLDecoder.decode(pair.substring(0, idx), "UTF-8") : pair;
            if (!query_pairs.containsKey(key)) {
                query_pairs.put(key, new LinkedList<>());
            }
            final String value = idx > 0 && pair.length() > idx + 1 ? URLDecoder.decode(pair.substring(idx + 1), "UTF-8") : null;
            query_pairs.get(key).add(value);
        }
        return query_pairs;
    }

    public static String messageJson(String message) {
        return new StringBuilder("{\"message\":\"" + message + "\"}").toString();
    }

    /* Internal */
    // ------------------------------------------------------------------------
    private static InputStream getRequestBodyStream(HttpRequest request, SessionInputBufferImpl buffer) throws HttpException {
        InputStream contentStream = null;
        if (request instanceof HttpEntityEnclosingRequest) {
            ContentLengthStrategy contentLengthStrategy = StrictContentLengthStrategy.INSTANCE;
            long len = contentLengthStrategy.determineLength(request);
            if (len == ContentLengthStrategy.CHUNKED) {
                contentStream = new ChunkedInputStream(buffer);
            } else if (len == ContentLengthStrategy.IDENTITY) {
                contentStream = new IdentityInputStream(buffer);
            } else {
                contentStream = new ContentLengthInputStream(buffer, len);
            }
        }
        return contentStream;
    }
}
