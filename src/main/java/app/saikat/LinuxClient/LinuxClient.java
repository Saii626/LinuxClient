package app.saikat.LinuxClient;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import com.google.gson.Gson;
import com.sun.jna.Library;
import com.sun.jna.Native;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import app.saikat.ConfigurationManagement.ConfigurationManagerInstanceHandler;
import app.saikat.ConfigurationManagement.interfaces.ConfigurationManager;
import app.saikat.NetworkManagement.NetworkManager;
import app.saikat.NetworkManagement.NetworkManagerInstanceHandler;
import app.saikat.NetworkManagement.Service;
import app.saikat.UrlManagement.UrlInstanceHandler;
import app.saikat.UrlManagement.UrlManager;
import app.saikat.UrlManagement.WebsocketMessages.Authentication;

public class LinuxClient {

    private ConfigurationManager configurationManager;
    private NetworkManager networkManager;
    private UrlManager urlManager;
    private Gson gson;

    private Logger logger = LoggerFactory.getLogger(this.getClass().getSimpleName());

    private interface CLibrary extends Library {
        CLibrary INSTANCE = (CLibrary) Native.load("c", CLibrary.class);

        int getpid();
    }

    public LinuxClient() throws IOException {
        logger.debug("Instantiating dependencies");

        File configFile = new File("LinuxClient.conf");        

        this.configurationManager = ConfigurationManagerInstanceHandler.createInstance(configFile);
        this.gson = ConfigurationManagerInstanceHandler.getGson();

        if (configurationManager.<Integer>get("pid").isPresent()) {
            logger.warn("An instance of LinuxClient already running with pid {}",
                    configurationManager.<Integer>getRaw("pid"));
            logger.warn("Exiting....");
            System.exit(0);
        } else {
            int pid = CLibrary.INSTANCE.getpid();
            logger.debug("Instance started with pid {}", pid);
            configurationManager.put("pid", pid);
            configurationManager.syncConfigurations();

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.debug("Shutting down LinuxClient instance");
                try {
                    configurationManager.delete("pid");
                    configurationManager.syncConfigurations();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }));
        }
        
        this.urlManager = UrlInstanceHandler.createInstance(configurationManager);
        WaspberryMessageHandlers messageHandlers = new WaspberryMessageHandlers();
        this.networkManager = NetworkManagerInstanceHandler.createInstanceWith(configurationManager, urlManager, gson, messageHandlers,
                Service.HTTP, Service.Websocket);

        setupWebsocket();
    }

    private void setupWebsocket() throws IOException {
        Authentication authentication = configurationManager.getOrSetDefault("authentication", new Authentication(UUID.randomUUID(), "default"));
        networkManager.connect(authentication);
        // networkManager.connect();
        // List<MessageModel> models = new ArrayList<>();
        // models.add(new Notification());

        // List<MessageHandler<? extends MessageModel>> handlers = new ArrayList<>();
        // handlers.add(new NotificationHandler());

        // networkManager.addWebsocketMessages(models);
        // networkManager.addWebsocketHandlers(handlers);
    }

    public void loop() {
        logger.debug("Infinite main loop");
        while (true) {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
    
    public static void main(String[] args) throws IOException {
        LinuxClient linuxClient = new LinuxClient();
        linuxClient.loop();
    }
}