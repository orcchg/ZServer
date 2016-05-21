package com.orcchg.zserver.server;

import com.orcchg.zserver.database.DatabaseHelper;
import com.orcchg.zserver.utility.Pair;
import com.orcchg.zserver.utility.Utility;
import hirondelle.date4j.DateTime;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import rx.Subscriber;
import rx.schedulers.Schedulers;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URL;
import java.util.SimpleTimeZone;
import java.util.TimeZone;

class WorkerRunnable implements Runnable {
    private static Logger sLogger = LogManager.getLogger(WorkerRunnable.class);

    private Socket mClientSocket = null;
    private DatabaseHelper mDbHelper;
    private String mDelimiter = "";

    WorkerRunnable(Socket clientSocket, DatabaseHelper dbHelper) {
        mClientSocket = clientSocket;
        mDbHelper = dbHelper;
    }

    public void run() {
        try {
            InputStream input = mClientSocket.getInputStream();
            OutputStream output = mClientSocket.getOutputStream();

            Pair<HttpRequest, InputStream> requestWithBody = Utility.getRequestFromConnection(input);
            HttpRequest request = requestWithBody.getKey();
            InputStream bodyStream = requestWithBody.getValue();

            StringBuilder content = new StringBuilder(Utility.CONTENT_LIST_OPEN);

            URL url = new URL("http://" + request.getFirstHeader("Host").getValue() + request.getRequestLine().getUri());
            Backend.invokeMethod(mDbHelper, url, bodyStream)
                    .subscribeOn(Schedulers.io())
                    .observeOn(Schedulers.immediate())
                    .subscribe(new Subscriber<String>() {
                        @Override
                        public void onCompleted() {
                            sLogger.debug("Request processed: " + System.currentTimeMillis());
                            content.append(Utility.CONTENT_LIST_CLOSE);
                            try {
                                sendResponse(output, content.toString());
                                input.close();
                                output.close();
                            } catch (IOException e) {
                                sLogger.error("IO exception during writing into output stream");
                                onError(e);
                            }
                        }

                        @Override
                        public void onError(Throwable error) {
                            error.printStackTrace();
                        }

                        @Override
                        public void onNext(String entity) {
                            content.append(mDelimiter).append(entity);
                            mDelimiter = ",";
                        }
                    });
        } catch (IOException | HttpException exception) {
            exception.printStackTrace();
        } catch (Backend.NoSuchMethodException exception) {
            sLogger.debug("No such method: " + exception.getMessage());
        }
    }

    private void sendResponse(OutputStream output, String body) throws IOException {
        int rawOffset = -8 * 60 * 60 * 1000;
        String[] ids = TimeZone.getAvailableIDs(rawOffset);
        TimeZone timeZone = new SimpleTimeZone(rawOffset, ids[0]);
        DateTime now = DateTime.now(timeZone);
        String dateTime = now.format("YYYY-MM-DD hh:mm:ss");

        output.write("HTTP/1.1 200 OK\r\n".getBytes());
        output.write(("Server: zserver-" + Utility.VERSION + "\r\n").getBytes());
        output.write(("Date: " + dateTime + "\r\n").getBytes());
        output.write("Content-Type: application/json\r\n".getBytes());
        output.write(("Content-Length: " + body.length() + "\r\n").getBytes());
        output.write("Connection: keep-alive\r\n".getBytes());
        output.write("Accept-Ranges: bytes\r\n\r\n".getBytes());
        output.write(body.getBytes());
    }
}
