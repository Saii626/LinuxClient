package app.saikat.LinuxClient;

import java.io.IOException;

import org.junit.Test;

import app.saikat.DIManagement.DIManager;

public class TestClient {

    @Test
    public void createInstance() throws IOException {
        DIManager.initialize("app.saikat");

        LinuxClient client = DIManager.get(LinuxClient.class);
        client.runWithArgs(new String[0]);
    }
}