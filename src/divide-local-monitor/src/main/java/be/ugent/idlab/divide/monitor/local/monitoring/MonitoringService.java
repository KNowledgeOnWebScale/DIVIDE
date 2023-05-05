package be.ugent.idlab.divide.monitor.local.monitoring;

import be.ugent.idlab.divide.monitor.local.ILocalMonitorService;
import be.ugent.idlab.divide.monitor.local.semanticmapper.SemanticMapper;
import be.ugent.idlab.rspservice.common.interfaces.RDFStream;
import be.ugent.idlab.rspservice.common.interfaces.RDFStreamProcessor;
import be.ugent.idlab.rspservice.common.util.InvalidRdfException;

public abstract class MonitoringService implements ILocalMonitorService {

    private final RDFStreamProcessor rdfStreamProcessor;
    private final RDFStream localMonitorStream;

    private final String componentId;
    private final String deviceId;

    protected MonitoringService(RDFStreamProcessor rdfStreamProcessor,
                                RDFStream localMonitorStream,
                                String componentId,
                                String deviceId) {
        this.rdfStreamProcessor = rdfStreamProcessor;
        this.localMonitorStream = localMonitorStream;
        this.componentId = componentId;
        this.deviceId = deviceId;
    }

    protected void handleMonitoringOutput(String monitorOutputJson) {
        // map JSON output to RDF
        String monitorOutputRdf = mapJsonToRdf(monitorOutputJson);

        // send RDF output to stream of Local Monitor RSP engine
        sendDataToStream(monitorOutputRdf);
    }

    private String mapJsonToRdf(String json) {
        return SemanticMapper.getInstance().mapJsonToRdf(json, componentId, deviceId);
    }

    private void sendDataToStream(String rdf) {
        try {
            this.rdfStreamProcessor.feedRDFStream(this.localMonitorStream, rdf);
        } catch (InvalidRdfException e) {
            throw new RuntimeException(e);
        }
    }

}
