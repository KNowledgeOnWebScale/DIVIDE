package be.ugent.idlab.divide.monitor.global.rs;

import be.ugent.idlab.divide.core.engine.IDivideEngine;
import be.ugent.idlab.divide.monitor.DivideGlobalMonitorConstants;
import be.ugent.idlab.divide.monitor.global.GlobalMonitorQuery;
import be.ugent.idlab.divide.monitor.global.IGlobalMonitorService;
import be.ugent.idlab.reasoningservice.common.ReasoningServer;
import be.ugent.idlab.reasoningservice.common.configuration.StaticConfig;
import be.ugent.idlab.reasoningservice.common.reasoning.ReasoningService;
import be.ugent.idlab.util.rdf.RDFLanguage;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class GlobalMonitorReasoningServiceFactory {

    public static IGlobalMonitorService createGlobalMonitorReasoningService(
            ReasoningServer reasoningServer,
            ReasoningService reasoningService,
            IDivideEngine divideEngine,
            List<GlobalMonitorQuery> globalMonitorQueries) {
        return new GlobalMonitorReasoningService(
                reasoningServer, reasoningService, divideEngine, globalMonitorQueries);
    }

    public static StaticConfig getReasoningServiceConfig(List<URL> ontologies) {
        // create and return config
        return new StaticConfig(
                "localhost",          // start on localhost
                DivideGlobalMonitorConstants.RS_SERVER_PORT, // start RSP server at given port
                DivideGlobalMonitorConstants.RS_SERVER_URI,  // reasoning service URI
                DivideGlobalMonitorConstants.RS_ID, // ID of reasoning server to be used in blank node URIs
                RDFLanguage.N_TRIPLE, // send results to observes in N-Triple format
                false,                // do not send empty results
                false,                // do not open WebSocket server for streaming data
                0,                    // port number for WebSocket server for streaming data (= irrelevant)
                true,                 // make the reasoning service streaming
                DivideGlobalMonitorConstants.RS_TUMBLING_WINDOW_SIZE_SECONDS,
                DivideGlobalMonitorConstants.RS_TUMBLING_WINDOW_SIZE_SECONDS, // execute queries on a tumbling window
                true,                 // perform the streaming with the Esper internal time,
                true,                 // delete all windowed events (coming from local monitor) after processing
                false,                // do not check overlap between windowed event & data store before
                                      // processing: monitor events will be distinct from other meta-model info
                true,                 // filter double triples in construct query result, to avoid duplicate
                                      // tasks to be sent to the DIVIDE engine
                false,                // do not check overlap in temporal additions (since there will be no temporal
                                      // additions - all scheduled queries execute in order but independently!)
                false,                // do not avoid deletion of triples inserted by query: queries will not
                                      // be adding to the datastore so that does not make sense
                ontologies,           // pass list of ontologies created from resources
                new ArrayList<>(),    // no additional data sources on top of ontology files (meta model context
                                      // will be added to data store through single run queries)
                false,                // log incoming events of global monitor
                false                 // no debugging mode
        );
    }

}
