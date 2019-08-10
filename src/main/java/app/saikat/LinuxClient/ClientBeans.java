package app.saikat.LinuxClient;

import java.io.File;

import app.saikat.ConfigurationManagement.impl.ConfigFile.ConfigFile;
import app.saikat.DIManagement.Provides;

class ClientBeans {

    @Provides
    @ConfigFile
    public File getConfigFile() {
        return new File("LinuxClient.conf");
    }

    @Provides
    public WaspberryMessageHandlers getMessageHandlers() {
        return new WaspberryMessageHandlers();
    }
}