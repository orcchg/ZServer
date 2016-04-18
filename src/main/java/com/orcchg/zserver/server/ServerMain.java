package com.orcchg.zserver.server;

public class ServerMain {

    public static void main(String[] args) {
        ServerLooper server = new ServerLooper(9000);
        new Thread(server).start();

        try {
            Thread.sleep(60 * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("Stopping Server");
        server.stop();
    }
}
