package com.orcchg.zserver.server;

import com.orcchg.zserver.database.DatabaseHelper;
import com.orcchg.zserver.utility.Utility;
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
import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

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

                List<String> entities = null;
                switch (path) {
                    case "/customer/":
                        params = Utility.splitQuery(url);
                        int limit = Integer.parseInt(params.get("limit").get(0));
                        int offset = Integer.parseInt(params.get("offset").get(0));
                        entities = mDbHelper.getCustomers(limit, offset);
                        break;
                    case "/address/":
                        params =Utility.splitQuery(url);
                        int addressId = Integer.parseInt(params.get("address_id").get(0));
                        entities = mDbHelper.getAddress(addressId);
                        break;
                }

                long time = System.currentTimeMillis();
                output.write(("HTTP/1.1 200 OK\r\nWorkerRunnable: " + mServerText + " - " + time + "\r\n\r\n").getBytes());
                output.write(requestString.getBytes());
                output.write("\r\n".getBytes());
                if (entities != null) {
                    for (String entity : entities) {
                        output.write(entity.getBytes());
                        output.write("\r\n".getBytes());
                    }
                }
                System.out.println("Request processed: " + time);

            } catch (SQLException e) {
                e.printStackTrace();
            } catch (HttpException e) {
                e.printStackTrace();
            }

            output.close();
            input.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
