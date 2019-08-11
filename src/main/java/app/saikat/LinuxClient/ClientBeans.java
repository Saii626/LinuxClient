package app.saikat.LinuxClient;

import java.io.File;

import app.saikat.ConfigurationManagement.interfaces.ConfigFile;
import app.saikat.DIManagement.Provides;
import app.saikat.PojoCollections.CommonObjects.WebsocketMessageHandlers;

class ClientBeans {

    @Provides
    @ConfigFile
    public File getConfigFile() {
        return new File("LinuxClient.conf");
    }

    @Provides
    public WebsocketMessageHandlers getMessageHandlers() {
        return new WaspberryMessageHandlers();
    }
}