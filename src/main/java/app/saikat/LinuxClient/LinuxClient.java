package app.saikat.LinuxClient;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.UUID;

import com.google.gson.Gson;
import com.sun.jna.Library;
import com.sun.jna.Native;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import app.saikat.ConfigurationManagement.ConfigurationManagerInstanceHandler;
import app.saikat.ConfigurationManagement.Gson.JsonObject;
import app.saikat.ConfigurationManagement.interfaces.ConfigurationManager;
import app.saikat.LinuxClient.MessageObejcts.Notify;
import app.saikat.LinuxClient.MessageObejcts.Status;
import app.saikat.NetworkManagement.NetworkManager;
import app.saikat.NetworkManagement.NetworkManagerInstanceHandler;
import app.saikat.NetworkManagement.Service;
import app.saikat.UrlManagement.UrlInstanceHandler;
import app.saikat.UrlManagement.UrlManager;
import app.saikat.UrlManagement.WebsocketMessages.ClientMessages.Authentication;

public class LinuxClient {

    private ConfigurationManager configurationManager;
    private NetworkManager networkManager;
    private UrlManager urlManager;
    private Gson gson;
    private WaspberryMessages waspberryMessages;

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    private int APPLICATION_PORT = 5000;

    private interface CLibrary extends Library {
        CLibrary INSTANCE = (CLibrary) Native.load("c", CLibrary.class);

        int getpid();
    }

    public LinuxClient(String[] args) throws IOException {
        logger.debug("Instantiating dependencies");

        File configFile = new File("LinuxClient.conf");

        this.configurationManager = ConfigurationManagerInstanceHandler.createInstance(configFile);
        this.gson = ConfigurationManagerInstanceHandler.getGson();

        ServerSocket serverSocket;
        try {
            serverSocket = new ServerSocket(APPLICATION_PORT);
        } catch (IOException e) {
            logger.info("Application already running on port {}", APPLICATION_PORT);
            processArgs(args);
            System.exit(0);
            return;
        }

        // if (configurationManager.<Integer>get("pid").isPresent()) {
        //     logger.warn("An instance of LinuxClient already running with pid {}",
        //             configurationManager.<Integer>getRaw("pid"));
        //     logger.warn("Exiting....");
        //     System.exit(0);
        // } else {
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
        // }

        this.urlManager = UrlInstanceHandler.createInstance(configurationManager);
        WaspberryMessageHandlers messageHandlers = new WaspberryMessageHandlers();
        this.networkManager = NetworkManagerInstanceHandler.createInstanceWith(configurationManager, urlManager, gson,
                messageHandlers, Service.HTTP, Service.Websocket);
        this.waspberryMessages = new WaspberryMessages(configurationManager, networkManager);

        setupWebsocket();

        logger.info("Application started listening on port {}", APPLICATION_PORT);
        // logger.info(processArgs(argumenList));
        startServer(serverSocket);
    }

    private void processArgs(String[] args) {
        ListIterator<String> iterator = Arrays.asList(args).listIterator();

        while (iterator.hasNext()) {
            String arg = iterator.next();
            logger.debug("Processing: {}", arg);
            if (arg.equals("--help") || arg.equals("-h")) {
                System.out.println("               \n"
                        + "--help     -h                                                     \t for help\n"
                        + "--status   -s    <arg>                                            \t for status\n"
                        + "--notify   -n    [target] <timeout=10> [title] [message]          \t for notify");
            } else if (arg.equals("--status") || arg.equals("-s")) {
                String target = iterator.hasNext() ? iterator.next() : "";
                Status status = new Status(target);

                this.sendToServer(status);
            } else if (arg.equals("--notify") || arg.equals("-n")) {
                try {
                    String target = iterator.next();
                    String title = iterator.next();

                    int ttl;
                    try {
                        ttl = Integer.parseInt(title);
                        title = iterator.next();
                    } catch (NumberFormatException e) {
                        ttl = 10;
                    }

                    String message = iterator.next();

                    Notify notify = new Notify(target, ttl, title, message);
                    this.sendToServer(notify);
                } catch (NoSuchElementException ex) {
                    System.out.println("Insufficient args. Use --help or -h");
                }

            } else {
                System.out.println("Unknown argument " + arg + "\n Use --help or -h for list of arguments");
            }
        }
    }

    private void startServer(ServerSocket serverSocket) {
        while (true) {
            try {
                Socket socket = serverSocket.accept();
                logger.info("Client connected");

                InputStream inputStream = socket.getInputStream();
                InputStreamReader iReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(iReader);

                StringBuffer result = new StringBuffer();
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    result.append(line);
                    result.append("\n");
                }
                String receivedMsg = result.toString();
                logger.info("Received: {}", receivedMsg);
                // inputStream.close();

                String response = "";
                JsonObject jsonObject = gson.fromJson(receivedMsg, JsonObject.class);
                if (jsonObject.getObject().getClass().getName().equals(Notify.class.getName())) {
                    Notify notify = (Notify) jsonObject.getObject();
                    boolean res = this.waspberryMessages.notifyDevice(notify.getTarget(), notify.getTitle(), notify.getMessage(), notify.getTtl());
                    response = String.format("Notify %s %s", notify.getTarget(), res ? "succeded" : "failed");
                } else {
                    response = String.format("%s not yet sopported", jsonObject.getObject().getClass().getSimpleName());
                }

                OutputStream outputStream = socket.getOutputStream();
                OutputStreamWriter oWriter = new OutputStreamWriter(outputStream);
                BufferedWriter bufferedWriter = new BufferedWriter(oWriter);

                bufferedWriter.write(response);
                bufferedWriter.flush();
                socket.close();

            } catch (IOException e) {
                logger.error("Error: ", e);
            }
        }

    }

    private <T> void sendToServer(T message) {

        JsonObject jsonObject = new JsonObject(message);
        String msg = gson.toJson(jsonObject);

        try {
            InetAddress address = InetAddress.getByName("localhost");
            Socket socket = new Socket(address, APPLICATION_PORT);

            OutputStream outputStream = socket.getOutputStream();
            OutputStreamWriter oWriter = new OutputStreamWriter(outputStream);
            BufferedWriter bufferedWriter = new BufferedWriter(oWriter);

            logger.debug("Sending to server");
            bufferedWriter.write(msg);
            bufferedWriter.flush();
            logger.info("Sent to server");
            // getOutputStream() doc
            socket.shutdownOutput();

            InputStream inputStream = socket.getInputStream();
            InputStreamReader iReader = new InputStreamReader(inputStream);
            BufferedReader bufferedReader = new BufferedReader(iReader);

            StringBuffer result = new StringBuffer();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                result.append(line);
                result.append("\n");
            }
            socket.close();

            logger.info("Server: \n{}", result.toString());
        } catch (IOException e) {
            logger.error("Error: ", e);
        }
    }

    private void setupWebsocket() throws IOException {
        Authentication authentication = configurationManager.getOrSetDefault("authentication",
                new Authentication(UUID.randomUUID(), "default"));

        networkManager.connect(authentication);
    }

    public static void main(String[] args) throws IOException {
        new LinuxClient(args);
    }
}