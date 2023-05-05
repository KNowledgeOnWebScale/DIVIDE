package be.ugent.idlab.divide.monitor.local.rsp;

import be.ugent.idlab.divide.monitor.local.ILocalMonitorService;
import be.ugent.idlab.divide.monitor.local.LocalMonitorException;
import be.ugent.idlab.divide.monitor.local.config.ILocalMonitorConfig;
import be.ugent.idlab.rspservice.common.RSPServer;
import be.ugent.idlab.rspservice.common.interfaces.RDFStreamProcessor;
import be.ugent.idlab.rspservice.common.observers.CustomContinuousObserver;
import be.ugent.idlab.rspservice.common.observers.HTTPObserver;
import be.ugent.idlab.rspservice.common.util.enumerations.QueryType;
import be.ugent.idlab.util.io.IOUtilities;
import be.ugent.idlab.util.rdf.QueryForm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

class LocalMonitorRspEngine implements ILocalMonitorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(LocalMonitorRspEngine.class.getName());

    private final RSPServer csparqlServer;
    private final RDFStreamProcessor rdfStreamProcessor;

    private final ILocalMonitorConfig localMonitorConfig;

    LocalMonitorRspEngine(ILocalMonitorConfig localMonitorConfig,
                          RSPServer csparqlServer,
                          RDFStreamProcessor rdfStreamProcessor) {
        this.csparqlServer = csparqlServer;
        this.rdfStreamProcessor = rdfStreamProcessor;

        this.localMonitorConfig = localMonitorConfig;
    }

    @Override
    public void start() throws LocalMonitorException {
        try {
            // start C-SPARQL server (static config)
            this.csparqlServer.start();

            // register global monitor as observer to all engine queries
            String centralMonitorReasoningServiceHost =
                    localMonitorConfig.getCentralMonitorReasoningServiceHost();
            int centralMonitorReasoningServicePort =
                    localMonitorConfig.getCentralMonitorReasoningServicePort();
            if (centralMonitorReasoningServiceHost == null || centralMonitorReasoningServicePort <= 0) {
                throw new LocalMonitorException("Global Monitor cannot be configured as observer: " +
                        "host and/or port of reasoning service are invalid/undefined");
            }
            String observerURL = String.format("%s://%s:%d%s/stream",
                    localMonitorConfig.getCentralMonitorReasoningServiceProtocol(),
                    centralMonitorReasoningServiceHost,
                    centralMonitorReasoningServicePort,
                    localMonitorConfig.getCentralMonitorReasoningServiceURI());
            LOGGER.info("Register global monitor's RSP engine as observer of " +
                    "all queries (url: {})", observerURL);
            this.rdfStreamProcessor.addObserver(new CustomContinuousObserver(
                    String.valueOf(observerURL.hashCode()),
                    new HTTPObserver(observerURL)));

            // register queries
            List<RspQuery> queries = getQueries();
            if (queries == null || queries.isEmpty()) {
                throw new LocalMonitorException(
                        "No queries available to run monitor -> makes setting up monitoring useless");
            }
            for (RspQuery query : queries) {
                LOGGER.info("Register query with name '{}' and body: {}", query.name, query.body);
                this.rdfStreamProcessor.registerQuery(
                        query.name, QueryType.QUERY, QueryForm.CONSTRUCT, query.body);
            }

        } catch (Exception e) {
            throw new LocalMonitorException("Error when starting local monitor's RSP engine", e);
        }
    }

    @Override
    public void reset() {
        // stop all services of the C-SPARQL RSP server
        try {
            this.csparqlServer.stop();
        } catch (Exception e) {
            LOGGER.warn("Resetting the local monitor RSP engine failed: " +
                    "C-SPARQL RSP server could not be stopped successfully", e);
        }
    }

    private List<RspQuery> getQueries() {
        List<RspQuery> rspQueries;
        String queryDirectory = "queries";
        try (final InputStream is = getClass().getClassLoader().getResourceAsStream(
                Paths.get(queryDirectory, "queries.txt").toString());
             final InputStreamReader isr =
                     new InputStreamReader(Objects.requireNonNull(is), StandardCharsets.UTF_8);
             final BufferedReader br = new BufferedReader(isr)) {
            rspQueries = br.lines()
                    .map(file -> new RspQuery(
                            file.split("\\.")[0],
                            IOUtilities.removeWhiteSpace(IOUtilities.readFileIntoString(
                                    getClass().getClassLoader().getResourceAsStream(
                                            Paths.get(queryDirectory, file).toString())))))
                    .collect(Collectors.toList());
            return rspQueries;

        } catch (IOException e) {
            return null;
        }
    }

    private static class RspQuery {

        private final String name;
        private final String body;

        public RspQuery(String name, String body) {
            this.name = name;
            this.body = String.format("REGISTER QUERY %s AS %s", name, body);
        }

    }

}
