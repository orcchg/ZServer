package com.orcchg.zserver.server;

import com.orcchg.zserver.database.DatabaseHelper;
import com.orcchg.zserver.utility.Pair;
import com.orcchg.zserver.utility.Utility;
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
            output.write(("HTTP/1.1 200 OK\r\n\r\n[").getBytes());

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
