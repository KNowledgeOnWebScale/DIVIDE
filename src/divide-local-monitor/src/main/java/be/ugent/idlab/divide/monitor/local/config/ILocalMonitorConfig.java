package be.ugent.idlab.divide.monitor.local.config;

public interface ILocalMonitorConfig {

    String getComponentId();

    String getDeviceId();

    boolean shouldStartDeviceMonitor();

    boolean shouldStartRspMonitor();

    boolean shouldStartNetworkMonitor();

    int getWebSocketPortOfLocalRspEngineMonitor();

    String getLocalPublicNetworkInterface();

    String getCentralMonitorReasoningServiceProtocol();

    String getCentralMonitorReasoningServiceHost();

    int getCentralMonitorReasoningServicePort();

    String getCentralMonitorReasoningServiceURI();

}
