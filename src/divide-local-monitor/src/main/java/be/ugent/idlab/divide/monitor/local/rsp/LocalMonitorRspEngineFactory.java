package be.ugent.idlab.divide.monitor.local.rsp;

import be.ugent.idlab.divide.monitor.local.ILocalMonitorService;
import be.ugent.idlab.divide.monitor.local.LocalMonitorConstants;
import be.ugent.idlab.divide.monitor.local.config.ILocalMonitorConfig;
import be.ugent.idlab.rspservice.common.RSPServer;
import be.ugent.idlab.rspservice.common.configuration.StaticConfig;
import be.ugent.idlab.rspservice.common.interfaces.RDFStreamProcessor;
import be.ugent.idlab.rspservice.common.util.enumerations.InferenceMode;
import be.ugent.idlab.util.rdf.RDFLanguage;

public class LocalMonitorRspEngineFactory {

    public static ILocalMonitorService createLocalMonitorRspEngine(
            ILocalMonitorConfig localMonitorConfig,
            RSPServer rspServer,
            RDFStreamProcessor rdfStreamProcessor) {
        return new LocalMonitorRspEngine(localMonitorConfig, rspServer, rdfStreamProcessor);
    }

    public static StaticConfig getRspEngineConfig() {
        return new StaticConfig(
                "localhost",          // start on localhost
                LocalMonitorConstants.RSP_ENGINE_SERVER_PORT, // start RSP server at given port
                true,                 // open WebSocket server for streaming data
                LocalMonitorConstants.RSP_ENGINE_WEBSOCKET_PORT, // open WebSocket server on given port
                null,                 // no resource path: queries execute on streaming data only
                LocalMonitorConstants.RSP_ENGINE_RESOURCES_PORT, // resources exposed on given port
                RDFLanguage.N_TRIPLE, // send results to observes in N-Triple format
                true,                 // enable timestamp function
                false,                // do not send empty results
                InferenceMode.NONE,   // do not perform reasoning during query evaluation
                null,                 // no TBox path used (since there is no reasoning)
                false,                // back loop not active: query results not put on new stream
                false,                // do not open WebSocket server with query results
                0,                    // port of WebSocket server with query results: not relevant
                null,                 // no list of rules used (since there is no reasoning)
                true,                 // defines whether incoming events should be logged
                false,                // defines whether outgoing events should be logged
                false                 // defines whether query results should be logged
        );
    }

}
