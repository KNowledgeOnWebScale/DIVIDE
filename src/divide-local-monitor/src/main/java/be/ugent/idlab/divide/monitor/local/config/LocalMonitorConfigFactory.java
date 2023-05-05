package be.ugent.idlab.divide.monitor.local.config;


import org.apache.commons.configuration2.ex.ConfigurationException;

import java.io.FileNotFoundException;

public class LocalMonitorConfigFactory {

    private static ILocalMonitorConfig instance;

    public static ILocalMonitorConfig getInstance() {
        return instance;
    }

    public static void initializeFromFile(String propertiesFilePath)
            throws ConfigurationException, FileNotFoundException {
        instance = new LocalMonitorFileConfig(propertiesFilePath);
    }

}
