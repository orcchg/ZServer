package com.orcchg.zserver.server;

import com.orcchg.zserver.database.DatabaseHelper;
import com.orcchg.zserver.utility.Utility;
import javafx.util.Pair;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import rx.Subscriber;
import rx.schedulers.Schedulers;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URL;

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
            InputStream input = mClientSocket.getInputStream();
            OutputStream output = mClientSocket.getOutputStream();
            output.write(("HTTP/1.1 200 OK\r\nWorkerRunnable: " + mServerText + " - " + System.currentTimeMillis() + "\r\n\r\n").getBytes());

            Pair<HttpRequest, InputStream> requestWithBody = Utility.getRequestFromConnection(input);
            HttpRequest request = requestWithBody.getKey();
            InputStream bodyStream = requestWithBody.getValue();

            URL url = new URL("http://" + request.getFirstHeader("Host").getValue() + request.getRequestLine().getUri());
            Backend.invokeMethod(mDbHelper, url)
                    .subscribeOn(Schedulers.io())
                    .observeOn(Schedulers.immediate())
                    .subscribe(new Subscriber<String>() {
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
                        public void onNext(String entity) {
                            try {
                                output.write("\r\n".getBytes());
                                output.write(entity.getBytes());
                                output.write("\r\n".getBytes());
                            } catch (IOException e) {
                                onError(e);
                            }
                        }
                    });
        } catch (IOException | HttpException | Backend.NoSuchMethodException exception) {
            exception.printStackTrace();
        }
    }
}
