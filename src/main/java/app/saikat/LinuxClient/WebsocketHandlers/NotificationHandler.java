package app.saikat.LinuxClient.WebsocketHandlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import app.saikat.LinuxClient.WebsocketModels.Notification;
import app.saikat.NetworkManagement.websocket.interfaces.MessageHandler;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class NotificationHandler implements MessageHandler<Notification> {

    private Logger logger = LoggerFactory.getLogger(this.getClass().getSimpleName());
    private ProcessBuilder processBuilder;

    public NotificationHandler() {
        processBuilder = new ProcessBuilder();
    }

    @Override
    public Class<Notification> registerForMessageModel() {
        return Notification.class;
    }

    @Override
    public void onMessage(Notification notification) {

        Thread processRunnerThread = new Thread(() -> {
            List<String> fullCommand = new ArrayList<>(getBaseCommandArgs());
            fullCommand.addAll(getTimeArgs(Duration.ofSeconds(5)));
            fullCommand.addAll(getMsgArgs(notification));

            processBuilder.command(fullCommand);

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

    private List<String> getBaseCommandArgs() {
        return Collections.singletonList("notify-send");
    }

    private List<String> getTimeArgs(Duration duration) {
        return Arrays.asList("-t", String.valueOf(duration.getSeconds()*1000));
    }

    private List<String> getMsgArgs(Notification notification) {
        return Arrays.asList(notification.title, notification.message);
    }
}
