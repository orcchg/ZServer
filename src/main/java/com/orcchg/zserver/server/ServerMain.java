package com.orcchg.zserver.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ServerMain {
    private static Logger sLogger = LogManager.getLogger(ServerMain.class);

    public static void main(String[] args) {
        ServerLooper server = new ServerLooper(9000);
        new Thread(server).start();

        try {
            Thread.sleep(20 * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        sLogger.debug("Stopping Server");
        server.stop();
    }
}
