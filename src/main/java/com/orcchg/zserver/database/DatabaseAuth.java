package com.orcchg.zserver.database;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

class DatabaseAuth {

    static String retrievePassword() {
        try (BufferedReader br = new BufferedReader(new FileReader("server_auth"))) {
            return br.readLine();
        } catch (IOException e) {
            System.out.println("Password file not found");
            e.printStackTrace();
        }
        return null;
    }
}
