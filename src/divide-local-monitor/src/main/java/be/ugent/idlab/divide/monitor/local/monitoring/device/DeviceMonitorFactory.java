package be.ugent.idlab.divide.monitor.local.monitoring.device;

import be.ugent.idlab.divide.monitor.local.ILocalMonitorService;
import be.ugent.idlab.rspservice.common.interfaces.RDFStream;
import be.ugent.idlab.rspservice.common.interfaces.RDFStreamProcessor;

public class DeviceMonitorFactory {

    public static ILocalMonitorService createDeviceMonitor(RDFStreamProcessor rdfStreamProcessor,
                                                           RDFStream localMonitorStream,
                                                           String componentId,
                                                           String deviceId) {
        return new DeviceMonitor(rdfStreamProcessor, localMonitorStream, componentId, deviceId);
    }

}
