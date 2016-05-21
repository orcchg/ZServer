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
            int rawOffset = -8 * 60 * 60 * 1000;
            String[] ids = TimeZone.getAvailableIDs(rawOffset);
            TimeZone timeZone = new SimpleTimeZone(rawOffset, ids[0]);
            DateTime now = DateTime.now(timeZone);
            String dateTime = now.format("YYYY-MM-DD hh:mm:ss");

            InputStream input = mClientSocket.getInputStream();
            OutputStream output = mClientSocket.getOutputStream();
            output.write("HTTP/1.1 200 OK\r\n".getBytes());
            output.write(("Server: zserver-" + Utility.VERSION + "\r\n").getBytes());
            output.write(("Date: " + dateTime + "\r\n").getBytes());
            output.write("Content-Type: application/json\r\n".getBytes());
            output.write(("Content-Length: " + 10000 + "\r\n").getBytes());
            output.write("Connection: keep-alive\r\n".getBytes());
            output.write("Accept-Ranges: bytes\r\n".getBytes());
            output.write("\r\n[".getBytes());

            Pair<HttpRequest, InputStream> requestWithBody = Utility.getRequestFromConnection(input);
            HttpRequest request = requestWithBody.getKey();
            InputStream bodyStream = requestWithBody.getValue();

            URL url = new URL("http://" + request.getFirstHeader("Host").getValue() + request.getRequestLine().getUri());
            Backend.invokeMethod(mDbHelper, url, bodyStream)
                    .subscribeOn(Schedulers.io())
                    .observeOn(Schedulers.immediate())
                    .subscribe(new Subscriber<String>() {
                        @Override
                        public void onCompleted() {
                            sLogger.debug("Request processed: " + System.currentTimeMillis());
                            try {
                                output.write("]".getBytes());
                                input.close();
                                output.close();
                            } catch (IOException e) {
                                onError(e);
                            }
                        }

                        @Override
                        public void onError(Throwable error) {
                            error.printStackTrace();
                        }

                        @Override
                        public void onNext(String entity) {
                            try {
                                output.write(mDelimiter.getBytes());
                                output.write(entity.getBytes());
                                mDelimiter = ",";
                                try {
                                    Thread.sleep(500);
                                } catch (InterruptedException e) {
                                    Thread.interrupted();
                                    onError(e);
                                }
                            } catch (IOException e) {
                                onError(e);
                            }
                        }
                    });
        } catch (IOException | HttpException exception) {
            exception.printStackTrace();
        } catch (Backend.NoSuchMethodException exception) {
            sLogger.debug("No such method: " + exception.getMessage());
        }
    }
}
