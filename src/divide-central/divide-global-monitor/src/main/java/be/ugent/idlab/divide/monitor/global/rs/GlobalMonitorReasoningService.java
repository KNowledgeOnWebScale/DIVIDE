package be.ugent.idlab.divide.monitor.global.rs;

import be.ugent.idlab.divide.core.engine.IDivideEngine;
import be.ugent.idlab.divide.monitor.global.GlobalMonitorException;
import be.ugent.idlab.divide.monitor.global.GlobalMonitorQuery;
import be.ugent.idlab.divide.monitor.global.IGlobalMonitorService;
import be.ugent.idlab.reasoningservice.common.ReasoningServer;
import be.ugent.idlab.reasoningservice.common.observer.CustomQueryObserver;
import be.ugent.idlab.reasoningservice.common.observer.QueryObserver;
import be.ugent.idlab.reasoningservice.common.query.Query;
import be.ugent.idlab.reasoningservice.common.reasoning.ReasoningService;
import be.ugent.idlab.reasoningservice.common.util.enumerations.AddQueryResultMode;
import be.ugent.idlab.util.rdf.QueryForm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

class GlobalMonitorReasoningService implements IGlobalMonitorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalMonitorReasoningService.class.getName());

    private final ReasoningServer reasoningServer;
    private final ReasoningService reasoningService;
    private final IDivideEngine divideEngine;
    private final List<GlobalMonitorQuery> queries;

    GlobalMonitorReasoningService(ReasoningServer reasoningServer,
                                  ReasoningService reasoningService,
                                  IDivideEngine divideEngine,
                                  List<GlobalMonitorQuery> queries) {
        this.reasoningServer = reasoningServer;
        this.reasoningService = reasoningService;
        this.divideEngine = divideEngine;
        this.queries = queries;
    }

    @Override
    public void start() throws GlobalMonitorException {
        try {
            LOGGER.info("Starting Global Monitor Reasoning Service");

            // start reasoning server (static config)
            LOGGER.info("Starting Reasoning Server of Global Monitor Reasoning Service");
            this.reasoningServer.start();

            // create global observer ID for each query
            String observerID = "translator-handler";

            // register queries
            for (GlobalMonitorQuery query : queries) {
                LOGGER.info("Register query with name '{}' and body: {}",
                        query.getName(), query.getBody());
                Query registeredQuery = this.reasoningService.registerQuery(
                        query.getName(), QueryForm.CONSTRUCT, query.getBody(), AddQueryResultMode.ADD_NOT);

                // register translator handler as observer to query
                LOGGER.info("Register global monitor's translator handler as observer of " +
                        "query with name '{}'", query.getName());
                QueryObserver observer = new CustomQueryObserver(
                        observerID,
                        registeredQuery,
                        new GlobalMonitorReasoningServiceEngineObserver(observerID, divideEngine));
                registeredQuery.addObserver(observer);
            }

            // let DIVIDE engine know that DIVIDE Meta Model (and its reasoning service & server)
            // have been successfully initialized
            divideEngine.onDivideMetaModelInitialized();

        } catch (Exception e) {
            throw new GlobalMonitorException("Error when starting local monitor's RSP engine", e);
        }
    }

    @Override
    public void reset() {
        // stop all services of the C-SPARQL RSP server
        try {
            this.reasoningServer.stop();
        } catch (Exception e) {
            LOGGER.warn("Resetting the global monitor RSP engine failed: " +
                    "C-SPARQL RSP server could not be stopped successfully", e);
        }
    }

}
