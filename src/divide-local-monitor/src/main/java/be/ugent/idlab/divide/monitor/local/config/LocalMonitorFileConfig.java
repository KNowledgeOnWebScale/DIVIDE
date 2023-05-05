package be.ugent.idlab.divide.monitor.local.config;

import be.ugent.idlab.divide.monitor.local.config.util.CustomJsonConfiguration;
import be.ugent.idlab.util.io.IOUtilities;
import org.apache.commons.configuration2.JSONConfiguration;
import org.apache.commons.configuration2.ex.ConfigurationException;

import java.io.FileNotFoundException;

/**
 * Configuration of the Local Monitor.
 */
class LocalMonitorFileConfig implements ILocalMonitorConfig {

    private static final String COMPONENT_ID = "component_id";
    private static final String DEVICE_ID = "device_id";

    private static final String MONITOR_RSP = "monitor.rsp";
    private static final String MONITOR_DEVICE = "monitor.device";
    private static final String MONITOR_NETWORK = "monitor.network";

    private static final String LOCAL_RSP_MONITOR_WEBSOCKET_PORT = "local.rsp_engine.monitor.ws_port";
    private static final String LOCAL_PUBLIC_NETWORK_INTERFACE = "local.public_network_interface";

    private static final String CENTRAL_MONITOR_RS_PROTOCOL = "central.monitor_reasoning_service.protocol";
    private static final String CENTRAL_MONITOR_RS_HOST = "central.monitor_reasoning_service.host";
    private static final String CENTRAL_MONITOR_RS_PORT = "central.monitor_reasoning_service.port";
    private static final String CENTRAL_MONITOR_RS_URI = "central.monitor_reasoning_service.uri";

    private final String propertiesFilePath;
    private final JSONConfiguration config;
    //private final String configFileDirectory;

    LocalMonitorFileConfig(String propertiesFilePath) throws
            ConfigurationException, FileNotFoundException {
        this.propertiesFilePath = propertiesFilePath;
        this.config = new CustomJsonConfiguration(propertiesFilePath);
        /*this.configFileDirectory = new File(propertiesFilePath)
                .getAbsoluteFile().getParentFile().getAbsolutePath();*/
    }

    @Override
    public String getComponentId() {
        return config.getString(COMPONENT_ID);
    }

    @Override
    public String getDeviceId() {
        return config.getString(DEVICE_ID);
    }

    @Override
    public boolean shouldStartDeviceMonitor() {
        return config.getBoolean(MONITOR_DEVICE, true);
    }

    @Override
    public boolean shouldStartRspMonitor() {
        return config.getBoolean(MONITOR_RSP, true);
    }

    @Override
    public boolean shouldStartNetworkMonitor() {
        return config.getBoolean(MONITOR_NETWORK, true);
    }

    @Override
    public int getWebSocketPortOfLocalRspEngineMonitor() {
        return config.getInt(LOCAL_RSP_MONITOR_WEBSOCKET_PORT, 54548);
    }

    @Override
    public String getLocalPublicNetworkInterface() {
        return config.getString(LOCAL_PUBLIC_NETWORK_INTERFACE);
    }

    @Override
    public String getCentralMonitorReasoningServiceProtocol() {
        return config.getString(CENTRAL_MONITOR_RS_PROTOCOL, "http");
    }

    @Override
    public String getCentralMonitorReasoningServiceHost() {
        return config.getString(CENTRAL_MONITOR_RS_HOST, null);
    }

    @Override
    public int getCentralMonitorReasoningServicePort() {
        return config.getInt(CENTRAL_MONITOR_RS_PORT, -1);
    }

    @Override
    public String getCentralMonitorReasoningServiceURI() {
        return config.getString(CENTRAL_MONITOR_RS_URI, "/globalmonitorreasoningservice");
    }

    @Override
    public String toString() {
        return "Local Monitor Config: " + IOUtilities.removeWhiteSpace(
                IOUtilities.readFileIntoString(propertiesFilePath));
    }

}
