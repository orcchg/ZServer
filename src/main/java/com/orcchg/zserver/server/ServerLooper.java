package com.orcchg.zserver.server;

import com.orcchg.zserver.database.DatabaseHelper;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerLooper implements Runnable {
    protected DatabaseHelper mDbHelper;

    protected int mServerPort = 8080;
    protected ServerSocket mServerSocket = null;

    protected boolean mIsStopped = false;
    protected Thread mWorkerThread = null;
    protected ExecutorService mThreadPool = Executors.newFixedThreadPool(10);

    public ServerLooper(int port) {
        mDbHelper = new DatabaseHelper();
        mServerPort = port;
    }

    public void run() {
        synchronized(this) {
            mWorkerThread = Thread.currentThread();
        }
        init();
        loop();
        mThreadPool.shutdown();
        System.out.println("Server Stopped.") ;
    }

    private synchronized boolean isStopped() {
        return mIsStopped;
    }

    public synchronized void stop() {
        mIsStopped = true;
        try {
            mServerSocket.close();
        } catch (IOException e) {
            throw new RuntimeException("Error closing server", e);
        }
    }

    private void openServerSocket() {
        try {
            mServerSocket = new ServerSocket(mServerPort);
        } catch (IOException e) {
            throw new RuntimeException("Cannot open port 8080", e);
        }
    }

    /* Server running */
    // ----------------------------------------------------------------------------------------------------------------
    private void init() {
        openServerSocket();
    }

    private void loop() {
        while (!isStopped()) {
            Socket clientSocket = null;
            try {
                clientSocket = mServerSocket.accept();
            } catch (IOException e) {
                if (isStopped()) {
                    System.out.println("Server Stopped.") ;
                    break;
                }
                throw new RuntimeException("Error accepting client connection", e);
            }
            mThreadPool.execute(new WorkerRunnable(clientSocket, mDbHelper, "Thread Pooled Server"));
        }
    }
}
