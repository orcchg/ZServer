package com.orcchg.zserver.server;

import com.orcchg.zserver.database.DatabaseHelper;

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
                result = mDbHelper.testQuery();
                System.out.println("Result: " + result.length());
            } catch (SQLException e) {
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
