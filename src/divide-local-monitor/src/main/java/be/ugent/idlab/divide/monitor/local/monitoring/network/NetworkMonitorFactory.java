package be.ugent.idlab.divide.monitor.local.monitoring.network;

import be.ugent.idlab.divide.monitor.local.ILocalMonitorService;
import be.ugent.idlab.rspservice.common.interfaces.RDFStream;
import be.ugent.idlab.rspservice.common.interfaces.RDFStreamProcessor;

public class NetworkMonitorFactory {

    public static ILocalMonitorService createNetworkMonitor(RDFStreamProcessor rdfStreamProcessor,
                                                            RDFStream localMonitorStream,
                                                            String componentId,
                                                            String deviceId,
                                                            String publicNetworkInterface,
                                                            String centralHost) {
        return new NetworkMonitor(rdfStreamProcessor, localMonitorStream,
                componentId, deviceId, publicNetworkInterface, centralHost);
    }

}
