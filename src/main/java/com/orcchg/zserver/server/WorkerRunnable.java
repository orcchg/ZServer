package com.orcchg.zserver.server;

import com.orcchg.zserver.database.DatabaseHelper;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.entity.ContentLengthStrategy;
import org.apache.http.impl.entity.StrictContentLengthStrategy;
import org.apache.http.impl.io.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.sql.SQLException;

public class WorkerRunnable implements Runnable {
    protected Socket mClientSocket = null;
    protected DatabaseHelper mDbHelper;
    protected String mServerText = null;

    public WorkerRunnable(Socket clientSocket, DatabaseHelper dbHelper, String serverText) {
        mClientSocket = clientSocket;
        mDbHelper = dbHelper;
        mServerText = serverText;
    }

    public void run() {
        try {
            InputStream input  = mClientSocket.getInputStream();
            OutputStream output = mClientSocket.getOutputStream();
            String result = "";
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

                System.out.println("Request: " + request.getRequestLine().getUri());
                if (contentStream != null) {
                    String body = IOUtils.toString(contentStream, "UTF-8");
                    System.out.println("Body: " + body);
                }
                result = mDbHelper.testQuery();
                System.out.println("Result: " + result.length());
            } catch (SQLException e) {
                e.printStackTrace();
            } catch (HttpException e) {
                e.printStackTrace();
            }
            long time = System.currentTimeMillis();
            output.write(("HTTP/1.1 200 OK\n\nWorkerRunnable: " + mServerText + " - " + time + "").getBytes());
            output.write(result.getBytes());
            output.close();
            input.close();
            System.out.println("Request processed: " + time);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
