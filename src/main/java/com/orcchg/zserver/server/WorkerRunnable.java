package com.orcchg.zserver.server;

import com.google.gson.Gson;
import com.orcchg.zserver.database.DatabaseHelper;
import com.orcchg.zserver.utility.Utility;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.entity.ContentLengthStrategy;
import org.apache.http.impl.entity.StrictContentLengthStrategy;
import org.apache.http.impl.io.*;
import rx.Subscriber;
import rx.schedulers.Schedulers;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URL;
import java.util.List;
import java.util.Map;

class WorkerRunnable implements Runnable {
    private Socket mClientSocket = null;
    private DatabaseHelper mDbHelper;
    private String mServerText = null;

    WorkerRunnable(Socket clientSocket, DatabaseHelper dbHelper, String serverText) {
        mClientSocket = clientSocket;
        mDbHelper = dbHelper;
        mServerText = serverText;
    }

    public void run() {
        try {
            InputStream input  = mClientSocket.getInputStream();
            OutputStream output = mClientSocket.getOutputStream();
            try {
                HttpTransportMetricsImpl metrics = new HttpTransportMetricsImpl();
                SessionInputBufferImpl buffer = new SessionInputBufferImpl(metrics, 2048);
                buffer.bind(input);
                DefaultHttpRequestParser parser = new DefaultHttpRequestParser(buffer);
                HttpRequest request = parser.parse();

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

                String requestString = "Request: " + request.getRequestLine().getUri();
                System.out.println(requestString);
                String authority = request.getFirstHeader("Host").getValue();
                if (contentStream != null) {
                    String body = IOUtils.toString(contentStream, "UTF-8");
                    System.out.println("Body: " + body);
                }

                URL url = new URL("http://" + authority + request.getRequestLine().getUri());
                String path = url.getPath();
                Map<String, List<String>> params;

                Gson gson = new Gson();
                Subscriber<String> subscriber = new Subscriber<String>() {
                    @Override
                    public void onCompleted() {
                        System.out.println("Request processed: " + System.currentTimeMillis());
                        try {
                            input.close();
                            output.close();
                        } catch (IOException e) {
                            onError(e);
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                    }

                    @Override
                    public void onNext(String s) {
                        try {
                            output.write("\r\n".getBytes());
                            output.write(s.getBytes());
                            output.write("\r\n".getBytes());
                        } catch (IOException e) {
                            onError(e);
                        }
                    }
                };

                long time = System.currentTimeMillis();
                output.write(("HTTP/1.1 200 OK\r\nWorkerRunnable: " + mServerText + " - " + time + "\r\n\r\n").getBytes());
                output.write(requestString.getBytes());

                switch (path) {
                    case "/customer/":
                        params = Utility.splitQuery(url);
                        int limit = Integer.parseInt(params.get("limit").get(0));
                        int offset = Integer.parseInt(params.get("offset").get(0));
                        mDbHelper.getCustomers(limit, offset)
                                .subscribeOn(Schedulers.io())
                                .observeOn(Schedulers.immediate())
                                .map(gson::toJson)
                                .subscribe(subscriber);
                        break;
                    case "/address/":
                        params =Utility.splitQuery(url);
                        int addressId = Integer.parseInt(params.get("address_id").get(0));
                        mDbHelper.getAddress(addressId)
                                .subscribeOn(Schedulers.io())
                                .observeOn(Schedulers.immediate())
                                .map(gson::toJson)
                                .subscribe(subscriber);
                        break;
                }
            } catch (HttpException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
