package be.ugent.idlab.divide;

import be.ugent.idlab.divide.api.DivideApiComponentFactory;
import be.ugent.idlab.divide.configuration.DivideConfig;
import be.ugent.idlab.divide.configuration.DivideQueryAsRspQlOrSparqlConfig;
import be.ugent.idlab.divide.configuration.DivideQueryConfig;
import be.ugent.idlab.divide.configuration.IDivideQueryConfig;
import be.ugent.idlab.divide.core.component.IComponent;
import be.ugent.idlab.divide.core.context.ContextEnrichment;
import be.ugent.idlab.divide.core.engine.DivideEngineFactory;
import be.ugent.idlab.divide.core.engine.IDivideEngine;
import be.ugent.idlab.divide.core.engine.IDivideQueryDeriver;
import be.ugent.idlab.divide.core.exception.DivideInvalidInputException;
import be.ugent.idlab.divide.core.exception.DivideNotInitializedException;
import be.ugent.idlab.divide.core.exception.DivideQueryDeriverException;
import be.ugent.idlab.divide.core.query.IDivideQuery;
import be.ugent.idlab.divide.core.query.parser.DivideQueryParserInput;
import be.ugent.idlab.divide.core.query.parser.DivideQueryParserOutput;
import be.ugent.idlab.divide.core.query.parser.InputQueryLanguage;
import be.ugent.idlab.divide.core.query.parser.InvalidDivideQueryParserInputException;
import be.ugent.idlab.divide.queryderivation.DivideQueryDeriverFactory;
import be.ugent.idlab.divide.queryderivation.DivideQueryDeriverType;
import be.ugent.idlab.divide.util.LogConstants;
import be.ugent.idlab.divide.util.component.ComponentEntry;
import be.ugent.idlab.divide.util.component.ComponentEntryParserException;
import be.ugent.idlab.divide.util.component.CsvComponentEntryParser;
import be.ugent.idlab.kb.IKnowledgeBase;
import be.ugent.idlab.kb.api.KbApiComponentFactory;
import be.ugent.idlab.kb.jena3.KnowledgeBaseFactory;
import be.ugent.idlab.kb.jena3.KnowledgeBaseType;
import be.ugent.idlab.util.io.IOUtilities;
import be.ugent.idlab.util.rdf.jena3.owlapi4.JenaUtilities;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DivideServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(DivideServer.class.getName());

    /**
     * Entry point of application: creates a DIVIDE server & starts it.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        try {
            if (args.length == 1 || args.length == 2) {
                new DivideServer().start(args);
            } else {
                System.out.println("Usage: DivideServer <configuration_file> [<components_file>]");
            }
        } catch (Exception e) {
            LOGGER.error("Error during DIVIDE server lifetime", e);
        }
    }

    private void start(String[] filePaths) throws Exception {
        // initialize Jena properly
        org.apache.jena.query.ARQ.init();

        // initialize logging
        System.setProperty("org.restlet.engine.loggerFacadeClass",
                "org.restlet.ext.slf4j.Slf4jLoggerFacade");

        // initialize configuration properties
        DivideConfig config;
        try {
            config = DivideConfig.getInstance(filePaths[0]);
        } catch (ConfigurationException | FileNotFoundException e) {
            LOGGER.error("Error while reading the configuration file {}", filePaths[0], e);
            System.out.println("Specified configuration file does not exist or is not valid");
            return;
        }

        // initialize knowledge base
        String baseIri = config.getBaseIriOfKnowledgeBase();
        KnowledgeBaseType knowledgeBaseType = config.getKnowledgeBaseType();
        IKnowledgeBase<Model> knowledgeBase = KnowledgeBaseFactory.getKnowledgeBase(
                knowledgeBaseType, baseIri);

        // create a DIVIDE query deriver that uses the EYE reasoner
        IDivideQueryDeriver divideQueryDeriver = DivideQueryDeriverFactory.
                createInstance(DivideQueryDeriverType.EYE,
                        config.shouldHandleTBoxDefinitionsInContext());

        // load DIVIDE ontology files
        LOGGER.info("Loading ontology...");
        Model divideOntologyModel = ModelFactory.createDefaultModel();
        for (String ontologyFile : config.getDivideOntologyFilePaths()) {
            LOGGER.info("-> ontology file: {}", ontologyFile);
            String fileContent = IOUtilities.readFileIntoString(ontologyFile);
            if (!fileContent.trim().isEmpty()) {
                Model model = JenaUtilities.parseString(fileContent);
                if (model != null) {
                    divideOntologyModel.add(model);
                } else {
                    throw new IllegalArgumentException(
                            String.format("Ontology file %s contains invalid RDF", ontologyFile));
                }
            }
        }

        // create and initialize DIVIDE engine
        IDivideEngine divideEngine = DivideEngineFactory.createInstance();
        divideEngine.initialize(
                divideQueryDeriver,
                knowledgeBase,
                divideOntologyModel,
                config.shouldStopRspEngineStreamsOnContextChanges(),
                config.shouldProcessUnmappedVariableMatchesInParser(),
                config.shouldValidateUnboundVariablesInRspQlQueryBodyInParser());

        // initialize list of DIVIDE queries in configuration
        // (wrongly configured DIVIDE queries lead to an IllegalArgumentException)
        LOGGER.debug(LogConstants.METRIC_MARKER, "INIT_QUERIES_START");
        initializeDivideQueries(divideEngine, config.getDivideQueryPropertiesFiles());
        initializeDivideQueriesAsRspQlOrSparql(
                divideEngine, config.getDivideQueryAsSparqlPropertiesFiles(),
                InputQueryLanguage.SPARQL);
        initializeDivideQueriesAsRspQlOrSparql(
                divideEngine, config.getDivideQueryAsRspQlPropertiesFiles(),
                InputQueryLanguage.RSP_QL);
        LOGGER.debug(LogConstants.METRIC_MARKER, "INIT_QUERIES_END");

        // initialize list of components in configuration (if specified)
        // (wrongly configured components lead to an IllegalArgumentException)
        if (filePaths.length > 1) {
            LOGGER.debug(LogConstants.METRIC_MARKER, "INIT_COMPONENTS_START");
            initializeComponents(divideEngine, filePaths[1]);
            LOGGER.debug(LogConstants.METRIC_MARKER, "INIT_COMPONENTS_END");
        }

        // create and start DIVIDE API
        DivideApiComponentFactory.createRestApiComponent(
                divideEngine, config.getHost(), config.getDivideServerPort(), "/divide").start();
        LOGGER.info("Started DIVIDE server API at http://{}:{}/divide",
                config.getHost(), config.getDivideServerPort());

        // create and start Knowledge Base API
        KbApiComponentFactory.createRestApiComponent(
                knowledgeBase, config.getHost(), config.getKnowledgeBaseServerPort(), "/kb").start();
        LOGGER.info("Started Knowledge Base server API at http://{}:{}/kb",
                config.getHost(), config.getKnowledgeBaseServerPort());
    }

    private void initializeDivideQueries(IDivideEngine divideEngine,
                                         List<String> divideQueryPropertiesFiles) {
        // loop over all specified properties files of a DIVIDE query
        for (String queryPropertiesFile : divideQueryPropertiesFiles) {
            String queryName = "";
            try {
                // read DIVIDE query properties file into config object
                DivideQueryConfig divideQueryConfig =
                        DivideQueryConfig.getInstance(queryPropertiesFile);

                // retrieve query name
                queryName = divideQueryConfig.getQueryName();

                // retrieve query pattern
                String queryPattern = IOUtilities.removeWhiteSpace(
                        IOUtilities.readFileIntoString(
                                divideQueryConfig.getQueryPatternFilePath()));
                if (queryPattern == null || queryPattern.trim().isEmpty()) {
                    throw new IllegalArgumentException(String.format(
                            "Query pattern file invalid or not specified for query '%s'", queryName));
                }

                // retrieve sensor query rule
                String sensorQueryRule = IOUtilities.removeWhiteSpace(
                        IOUtilities.readFileIntoString(
                                divideQueryConfig.getSensorQueryRuleFilePath()));
                if (sensorQueryRule == null || sensorQueryRule.trim().isEmpty()) {
                    throw new IllegalArgumentException(String.format(
                            "Sensor query rule file invalid or not specified for query '%s'", queryName));
                }

                // retrieve goal
                String goal = IOUtilities.removeWhiteSpace(
                        IOUtilities.readFileIntoString(divideQueryConfig.getGoalFilePath()));
                if (goal == null || goal.trim().isEmpty()) {
                    throw new IllegalArgumentException(String.format(
                            "Goal file invalid or not specified for query '%s'", queryName));
                }

                // retrieve context enrichment
                ContextEnrichment contextEnrichment = initializeContextEnrichment(
                        divideQueryConfig, queryName);

                // create and add DIVIDE query to the DIVIDE engine
                IDivideQuery divideQuery = divideEngine.addDivideQuery(
                        queryName, queryPattern, sensorQueryRule, goal, contextEnrichment);
                if (divideQuery == null) {
                    throw new IllegalArgumentException(String.format(
                            "DIVIDE query with name '%s' already exists", queryName));
                }

            } catch (ConfigurationException | FileNotFoundException e) {
                LOGGER.error("Error while reading the DIVIDE query properties file '{}'",
                        queryPropertiesFile, e);
                throw new IllegalArgumentException(e);

            } catch (IllegalArgumentException e) {
                LOGGER.error("Error in configuration of DIVIDE query '{}'", queryName, e);
                throw e;

            } catch (DivideNotInitializedException e) {
                LOGGER.error("DIVIDE engine is not properly initialized", e);
                throw new IllegalStateException(e);

            } catch (DivideInvalidInputException e) {
                LOGGER.error("Error when registering new DIVIDE query because input is invalid", e);
                throw new RuntimeException(e);

            } catch (DivideQueryDeriverException e) {
                LOGGER.error("Error when registering new query at query deriver", e);
                throw new RuntimeException(e);
            }
        }
    }

    private void initializeDivideQueriesAsRspQlOrSparql(IDivideEngine divideEngine,
                                                        List<String> divideQueryPropertiesFiles,
                                                        InputQueryLanguage inputQueryLanguage) {
        // loop over all specified properties files of a DIVIDE query
        for (String queryPropertiesFile : divideQueryPropertiesFiles) {
            String queryName = "";
            try {
                LOGGER.info("Trying to add new DIVIDE query from properties file {} ({} input) ",
                        queryPropertiesFile, inputQueryLanguage.toString());

                // read DIVIDE query properties file into config object
                DivideQueryAsRspQlOrSparqlConfig divideQueryConfig =
                        DivideQueryAsRspQlOrSparqlConfig.getInstance(queryPropertiesFile);

                // retrieve query name
                queryName = divideQueryConfig.getQueryName();

                // retrieve stream query
                String streamQuery = null;
                if (!divideQueryConfig.getStreamQueryFilePath().isEmpty()) {
                    streamQuery = IOUtilities.removeWhiteSpace(
                            IOUtilities.readFileIntoString(
                                    divideQueryConfig.getStreamQueryFilePath()));
                    if (streamQuery == null || streamQuery.trim().isEmpty()) {
                        throw new IllegalArgumentException(String.format(
                                "Stream query file invalid or not specified for query '%s'", queryName));
                    }
                }

                // retrieve intermediate queries
                List<String> intermediateQueries = new ArrayList<>();
                for (String intermediateQueryFilePath :
                        divideQueryConfig.getIntermediateQueryFilePaths()) {
                    String intermediateQuery = IOUtilities.removeWhiteSpace(
                            IOUtilities.readFileIntoString(intermediateQueryFilePath));
                    if (intermediateQuery == null || intermediateQuery.trim().isEmpty()) {
                        throw new IllegalArgumentException(String.format(
                                "Intermediate query file(s) specified for query '%s'" +
                                        "are non-existent, invalid or empty ", queryName));
                    }
                    intermediateQueries.add(intermediateQuery);
                }

                // retrieve final query
                String finalQuery = null;
                if (!divideQueryConfig.getFinalQueryFilePath().isEmpty()) {
                    finalQuery = IOUtilities.removeWhiteSpace(
                            IOUtilities.readFileIntoString(
                                    divideQueryConfig.getFinalQueryFilePath()));
                    if (finalQuery == null || finalQuery.trim().isEmpty()) {
                        throw new IllegalArgumentException(String.format(
                                "Final query file invalid or not specified for query '%s'", queryName));
                    }
                }

                // retrieve variable mapping of stream to final query
                Map<String, String> streamToFinalQueryVariableMapping;
                try {
                    streamToFinalQueryVariableMapping =
                            divideQueryConfig.getStreamToFinalQueryVariableMapping();
                } catch (ConfigurationException e) {
                    throw new IllegalArgumentException(
                            String.format("Stream to final query variable mapping " +
                                    "invalid for query '%s'", queryName), e);
                }

                // parse DIVIDE query input
                LOGGER.debug(LogConstants.METRIC_MARKER, "QUERY_PARSING_START");
                DivideQueryParserInput divideQueryParserInput = new DivideQueryParserInput(
                        inputQueryLanguage,
                        divideQueryConfig.getStreamWindows(),
                        streamQuery,
                        intermediateQueries,
                        finalQuery,
                        divideQueryConfig.getSolutionModifier(),
                        streamToFinalQueryVariableMapping);
                DivideQueryParserOutput divideQueryParserOutput =
                        divideEngine.getQueryParser().
                                parseDivideQuery(divideQueryParserInput);
                LOGGER.debug(LogConstants.METRIC_MARKER, "QUERY_PARSING_END");

                // retrieve context enrichment
                ContextEnrichment contextEnrichment = initializeContextEnrichment(
                        divideQueryConfig, queryName);

                // create and add DIVIDE query to the DIVIDE engine
                IDivideQuery divideQuery = divideEngine.addDivideQuery(
                        queryName,
                        divideQueryParserOutput.getQueryPattern(),
                        divideQueryParserOutput.getSensorQueryRule(),
                        divideQueryParserOutput.getGoal(),
                        contextEnrichment);
                if (divideQuery == null) {
                    throw new IllegalArgumentException(String.format(
                            "DIVIDE query with name '%s' already exists", queryName));
                }

                LOGGER.info("Successfully added new DIVIDE query '{}'", queryName);

            } catch (ConfigurationException | FileNotFoundException e) {
                LOGGER.error("Error while reading the DIVIDE query properties file '{}'",
                        queryPropertiesFile, e);
                throw new IllegalArgumentException(e);

            } catch (IllegalArgumentException e) {
                LOGGER.error("Error in configuration of DIVIDE query '{}'", queryName, e);
                throw e;

            } catch (DivideNotInitializedException e) {
                LOGGER.error("DIVIDE engine is not properly initialized", e);
                throw new IllegalStateException(e);

            } catch (InvalidDivideQueryParserInputException e) {
                LOGGER.error("Error when parsing DIVIDE query input", e);
                throw new RuntimeException(e);

            } catch (DivideInvalidInputException e) {
                LOGGER.error("Error when registering new DIVIDE query because input is invalid", e);
                throw new RuntimeException(e);

            } catch (DivideQueryDeriverException e) {
                LOGGER.error("Error when registering new query at query deriver", e);
                throw new RuntimeException(e);
            }
        }
    }

    private void initializeComponents(IDivideEngine divideEngine,
                                      String componentsFile) {
        try {
            // parse component entries specified in file
            List<ComponentEntry> componentEntries =
                    CsvComponentEntryParser.parseComponentEntryFile(componentsFile);

            // register all components to the DIVIDE engine
            for (ComponentEntry componentEntry : componentEntries) {
                IComponent component = divideEngine.registerComponent(
                        new ArrayList<>(componentEntry.getContextIris()),
                        componentEntry.getRspQueryLanguage(),
                        componentEntry.getRspEngineUrl());
                if (component == null) {
                    throw new IllegalArgumentException(
                            "Components file contains invalid or duplicate entries");
                }
            }

        } catch (ComponentEntryParserException | DivideInvalidInputException e) {
            String message = "Specified components file contains invalid inputs";
            LOGGER.error(message, e);
            throw new IllegalArgumentException(message, e);

        } catch (DivideNotInitializedException e) {
            // can be ignored - will never occur since server first explicitly
            // initializes the DIVIDE engine
        }
    }

    private ContextEnrichment initializeContextEnrichment(IDivideQueryConfig config,
                                                          String queryName) {
        // retrieve list of queries
        List<String> queries = new ArrayList<>();
        for (String queryFilePath :
                config.getContextEnrichmentQueryFilePaths()) {
            String query = IOUtilities.removeWhiteSpace(
                    IOUtilities.readFileIntoString(queryFilePath));
            if (query == null || query.trim().isEmpty()) {
                throw new IllegalArgumentException(String.format(
                        "Context-enriching query file(s) specified for query '%s'" +
                                "are non-existent, invalid or empty ", queryName));
            }
            queries.add(query);
        }

        // return basic context enrichment if no queries are present
        if (queries.isEmpty()) {
            return new ContextEnrichment();
        } else {
            // otherwise, retrieve context enrichment settings
            boolean doReasoning = config.getContextEnrichmentDoReasoning();
            boolean executeOnOntologyTriples = config.getContextEnrichmentExecuteOnOntologyTriples();

            // -> and create context enrichment
            return new ContextEnrichment(doReasoning, executeOnOntologyTriples, queries);
        }
    }

}
