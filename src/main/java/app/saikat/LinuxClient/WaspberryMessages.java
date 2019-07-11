package app.saikat.LinuxClient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import app.saikat.Annotations.WaspberryMessageHandler;
import app.saikat.UrlManagement.WebsocketMessages.AuthenticationResponse;

public class WaspberryMessages {

    private static Logger logger = LoggerFactory.getLogger(WaspberryMessages.class);

    @WaspberryMessageHandler
    public static void authResponse(AuthenticationResponse response) {

        switch(response.getStatus()) {
            case SUCCESS:
            logger.info("Successfully authenticated");
            break;
            case FAILED:
            logger.error("Authentication failed");
            break;
            default:
            logger.warn("Not expected");
        }
        
    }
}