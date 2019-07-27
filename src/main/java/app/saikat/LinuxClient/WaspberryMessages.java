package app.saikat.LinuxClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import app.saikat.Annotations.WaspberryMessageHandler;
import app.saikat.ConfigurationManagement.interfaces.ConfigurationManager;
import app.saikat.NetworkManagement.websocket.interfaces.WebsocketManager;
import app.saikat.PojoCollections.WebsocketMessages.ClientMessages.GetDeviceList;
import app.saikat.PojoCollections.WebsocketMessages.ClientMessages.NotifyDevices;
import app.saikat.PojoCollections.WebsocketMessages.ServerMessages.AuthenticationResponse;
import app.saikat.PojoCollections.WebsocketMessages.ServerMessages.DeviceList;
import app.saikat.PojoCollections.WebsocketMessages.ServerMessages.Notification;

public class WaspberryMessages {

    private DeviceList deviceList;
    private ConfigurationManager configurationManager;
    private WebsocketManager manager;

    private static WaspberryMessages instance;
    private static Logger logger = LoggerFactory.getLogger(WaspberryMessages.class);

    public WaspberryMessages(ConfigurationManager configurationManager, WebsocketManager manager) throws IOException {
        instance = this;
        this.configurationManager = configurationManager;
        this.manager = manager;

        this.deviceList = configurationManager.getOrSetDefault("deviceList", new DeviceList(new ArrayList<>()));
    }

    @WaspberryMessageHandler
    public static void authResponse(WebsocketManager manager, AuthenticationResponse response) {

        if (instance != null) {
            switch (response.getStatus()) {
            case SUCCESS:
                logger.info("Successfully authenticated");
                logger.debug("Requesting deviceList from server");
                manager.send(new GetDeviceList());

                break;
            case FAILED:
                logger.error("Authentication failed");
                break;
            default:
                logger.warn("Not expected");
            }
        }
    }

    @WaspberryMessageHandler
    public static void onDeviceList(DeviceList deviceList) {
        if (instance != null) {
            logger.info("Received devices: {}", Arrays.toString(deviceList.getDevices().toArray()));

            instance.deviceList = deviceList;
            instance.configurationManager.put("deviceList", instance.deviceList);
        }
    }

    @WaspberryMessageHandler
    public static void notification(Notification notification) {
        ProcessBuilder processBuilder = new ProcessBuilder();

        Thread processRunnerThread = new Thread(() -> {
            String[] notifyCommand = {"notify-send", "-t", "5000", notification.getTitle(), notification.getMessage()};

            processBuilder.command(Arrays.asList(notifyCommand));

            try {
                logger.debug("Executing notify-send command");
                Process process = processBuilder.start();

                int exitVal = process.waitFor();
                if (exitVal == 0) {
                    logger.debug("Successfully ran notify-send");
                } else {
                    logger.error("notify-send exited with non-zero exit code {}", exitVal);
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        });

        processRunnerThread.setName("notify-send");
        processRunnerThread.start();

    }

    // public void refreshDeviceList() {
    //     synchronized (this.deviceList) {
    //         if (!this.isUpdatingDeviceList) {
    //             this.isUpdatingDeviceList = true;
    //             this.manager.send(new GetDeviceList());
    //         }

    //         while (this.isUpdatingDeviceList) {
    //             try {
    //                 this.deviceList.wait(2000);
    //                 logger.debug("Devicelist not received after 2sec");
    //             } catch (InterruptedException e) {
    //                 logger.error("Error: ", e);
    //             }
    //         }
    //     }
    // }

    public boolean notifyDevice(String deviceName, String title, String message) {
        return this.notifyDevice(deviceName, title, message, 0);
    }

    public boolean notifyDevice(String deviceName, String title, String message, int ttl) {
        NotifyDevices notifyDevices = new NotifyDevices("thinkpad");
        Notification notification = new Notification(UUID.randomUUID(), new Date(), title, message, "thinkpad");
        notifyDevices.add(deviceName, notification, ttl);
        return this.manager.send(notifyDevices);
    }
}