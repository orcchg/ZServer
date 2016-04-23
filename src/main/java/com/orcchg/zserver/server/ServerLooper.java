package com.orcchg.zserver.server;

import com.orcchg.zserver.database.DatabaseHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class ServerLooper implements Runnable {
    private static Logger sLogger = LogManager.getLogger(ServerLooper.class);

    private DatabaseHelper mDbHelper;

    private int mServerPort = 8080;
    private ServerSocket mServerSocket = null;

    private boolean mIsStopped = false;
    private ExecutorService mThreadPool = Executors.newFixedThreadPool(10);

    ServerLooper(int port) {
        mDbHelper = new DatabaseHelper();
        mServerPort = port;
    }

    public void run() {
        init();
        loop();
        mThreadPool.shutdown();
        sLogger.debug("Server Stopped."); ;
    }

    private synchronized boolean isStopped() {
        return mIsStopped;
    }

    synchronized void stop() {
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
            Socket clientSocket;
            try {
                clientSocket = mServerSocket.accept();
                sLogger.debug("Accepted incoming connection: " + clientSocket.getInetAddress().getHostAddress());
            } catch (IOException e) {
                if (isStopped()) {
                    sLogger.debug("Server Stopped.") ;
                    break;
                }
                throw new RuntimeException("Error accepting client connection", e);
            }
            mThreadPool.execute(new WorkerRunnable(clientSocket, mDbHelper));
        }
    }
}
