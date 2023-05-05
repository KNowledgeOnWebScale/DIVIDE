package be.ugent.idlab.divide.monitor.local.monitoring.rsp;

import be.ugent.idlab.divide.monitor.local.ILocalMonitorService;
import be.ugent.idlab.rspservice.common.interfaces.RDFStream;
import be.ugent.idlab.rspservice.common.interfaces.RDFStreamProcessor;

public class RspMonitorFactory {

    public static ILocalMonitorService createRspMonitor(RDFStreamProcessor rdfStreamProcessor,
                                                        RDFStream localMonitorStream,
                                                        String componentId,
                                                        String deviceId,
                                                        int rspEngineMonitorWebSocketUrl) {
        return new RspMonitor(rdfStreamProcessor, localMonitorStream,
                componentId, deviceId, rspEngineMonitorWebSocketUrl);
    }

}
