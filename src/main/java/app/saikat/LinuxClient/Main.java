package app.saikat.LinuxClient;

import java.io.IOException;

import app.saikat.DIManagement.DIManager;

public class Main {

    public static void main(String[] args) throws IOException {
        DIManager.initialize("app.saikat");

        LinuxClient client = DIManager.get(LinuxClient.class);
        client.runWithArgs(args);
    }
}