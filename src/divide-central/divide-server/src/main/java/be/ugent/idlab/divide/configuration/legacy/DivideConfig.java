package be.ugent.idlab.divide.configuration.legacy;

import be.ugent.idlab.kb.jena3.KnowledgeBaseType;
import be.ugent.idlab.util.io.IOUtilities;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Configuration of the DIVIDE server (including engine, DIVIDE API
 * and Knowledge Base API configuration parameters).
 */
@SuppressWarnings("unused")
public class DivideConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(DivideConfig.class);

    private static final String DIVIDE_SERVER_HOST = "server.host";
    private static final String DIVIDE_SERVER_PORT_DIVIDE = "server.port.divide";
    private static final String DIVIDE_SERVER_PORT_KB = "server.port.kb";

    private static final String DIVIDE_KB_TYPE = "divide.kb.type";
    private static final String DIVIDE_KB_BASE_IRI = "divide.kb.base_iri";

    private static final String DIVIDE_ENGINE_STOP_RSP_ENGINE_STREAMS_ON_CONTEXT_CHANGES =
            "divide.engine.stop_rsp_engine_streams_on_context_changes";
    private static final String DIVIDE_ENGINE_PARSER_PROCESS_UNMAPPED_VARIABLE_MATCHES =
            "divide.engine.parser.process_unmapped_variable_matches";
    private static final String DIVIDE_ENGINE_PARSER_VALIDATE_UNBOUND_VARIABLES_IN_RSP_QL_QUERY_BODY =
            "divide.engine.parser.validate_unbound_variables_in_rsp-ql_query_body";

    private static final String DIVIDE_REASONER_HANDLE_TBOX_DEFINITIONS_IN_CONTEXT =
            "divide.reasoner.handle_tbox_definitions_in_context";

    private static final String DIVIDE_ONTOLOGY_DIRECTORY = "divide.ontology.dir";
    private static final String DIVIDE_ONTOLOGY = "divide.ontology";

    private static final String DIVIDE_QUERIES = "divide.queries";
    private static final String DIVIDE_QUERIES_AS_SPARQL = "divide.queries.sparql";
    private static final String DIVIDE_QUERIES_AS_RSP_QL = "divide.queries.rspql";

    private final Configuration config;
    private final String configFileDirectory;

    private DivideConfig(String propertiesFilePath) throws ConfigurationException {
        config = new PropertiesConfiguration(propertiesFilePath);
        configFileDirectory = new File(propertiesFilePath)
                .getAbsoluteFile().getParentFile().getAbsolutePath();
    }

    /**
     * Creates a DIVIDE config object based on the given properties file.
     *
     * @param propertiesFile path to properties file
     * @return an instantiated DIVIDE config object which can be used to retrieve
     *         the configuration parameters of the DIVIDE server
     * @throws ConfigurationException if the properties file does not exist or is invalid
     */
    public static DivideConfig getInstance(String propertiesFile)
            throws ConfigurationException {
        return new DivideConfig(propertiesFile);
    }

    /**
     * @return host on which the DIVIDE API & Knowledge Base API should be exposed
     *         (default: 'localhost')
     */
    public String getHost() {
        return config.getString(DIVIDE_SERVER_HOST, "localhost");
    }

    /**
     * @return port on which the DIVIDE API should be exposed (default: 5000)
     */
    public int getDivideServerPort() {
        return config.getInt(DIVIDE_SERVER_PORT_DIVIDE, 5000);
    }

    /**
     * @return port on which the Knowledge Base API should be exposed
     *         (default: 5001)
     */
    public int getKnowledgeBaseServerPort() {
        return config.getInt(DIVIDE_SERVER_PORT_KB, 5001);
    }

    /**
     * @return {@link KnowledgeBaseType} representing the type of knowledge base
     *         that should be instantiated for the DIVIDE engine
     *         (default: {@link KnowledgeBaseType#JENA})
     */
    public KnowledgeBaseType getKnowledgeBaseType() {
        KnowledgeBaseType defaultType = KnowledgeBaseType.JENA;
        KnowledgeBaseType configType =
                KnowledgeBaseType.fromString(config.getString(DIVIDE_KB_TYPE));
        return configType != null ? configType : defaultType;
    }

    /**
     * @return base IRI of the knowledge base that should be used for resolving
     *         IRIs used within the context of the started DIVIDE engine
     *         (default: 'http://idlab.ugent.be/divide')
     */
    public String getBaseIriOfKnowledgeBase() {
        return config.getString(DIVIDE_KB_BASE_IRI, "http://idlab.ugent.be/divide");
    }

    /**
     * @return whether DIVIDE should pause RSP engine streams on a component
     *         when context changes are detected that trigger the DIVIDE query
     *         derivation for that component (default: true)
     */
    public boolean shouldStopRspEngineStreamsOnContextChanges() {
        return config.getBoolean(DIVIDE_ENGINE_STOP_RSP_ENGINE_STREAMS_ON_CONTEXT_CHANGES, true);
    }

    /**
     * @return whether the DIVIDE query parser should process unmapped variable matches in the
     *         query input (i.e., identical variable names occurring in both the stream and
     *         final query, that are not explicitly defined in the variable mapping file of
     *         the input of that query) - if true, matching variable names are considered as
     *         variable mappings; if false, matching variable names are considered coincidence
     *         and are not treated as mappings (default: false)
     */
    public boolean shouldProcessUnmappedVariableMatchesInParser() {
        return config.getBoolean(DIVIDE_ENGINE_PARSER_PROCESS_UNMAPPED_VARIABLE_MATCHES, false);
    }

    /**
     * @return whether the DIVIDE query parser should validate variables in the RSP-QL query
     * body generated by the DIVIDE query parser, should be validated (= checked for occurrence
     * in the WHERE clause of the query or in the set of input variables that will be substituted
     * during the DIVIDE query derivation) during parsing (default: true)
     */
    public boolean shouldValidateUnboundVariablesInRspQlQueryBodyInParser() {
        return config.getBoolean(
                DIVIDE_ENGINE_PARSER_VALIDATE_UNBOUND_VARIABLES_IN_RSP_QL_QUERY_BODY, true);
    }

    /**
     * @return whether DIVIDE should allow to specify TBox definitions in the
     *         context updates sent for the query derivation; if true, this means
     *         that DIVIDE should scan all context updates for new OWL-RL axioms
     *         and rules upon each query derivation call, heavily impacting the
     *         derivation of queries upon context updates (default: false)
     */
    public boolean shouldHandleTBoxDefinitionsInContext() {
        return config.getBoolean(DIVIDE_REASONER_HANDLE_TBOX_DEFINITIONS_IN_CONTEXT, false);
    }

    /**
     * @return list of canonical path names of files containing the ontology (TBox) data
     *         used by this DIVIDE engine (default: empty list)
     */
    public List<String> getDivideOntologyFilePaths() {
        String ontologyDirectoryPath = config.getString(DIVIDE_ONTOLOGY_DIRECTORY, ".");
        if (ontologyDirectoryPath != null && !Paths.get(ontologyDirectoryPath).isAbsolute()) {
            ontologyDirectoryPath = Paths.get(configFileDirectory, ontologyDirectoryPath).toString();
        }

        String[] ontologyFileNames = config.getStringArray(DIVIDE_ONTOLOGY);

        List<String> ontologyFilePaths = new ArrayList<>();
        for (String ontologyFileName : ontologyFileNames) {
            try {
                File ontologyFile = new File(ontologyFileName);
                if (!ontologyFile.isAbsolute()) {
                    ontologyFile = new File(ontologyDirectoryPath, ontologyFileName);
                }

                String canonicalPath = ontologyFile.getCanonicalPath();
                if (IOUtilities.isValidFile(canonicalPath)) {
                    ontologyFilePaths.add(canonicalPath);
                } else {
                    throw new IOException(String.format("%s is not a valid file", canonicalPath));
                }
            } catch (IOException e) {
                LOGGER.error("Error with finding ontology file {}", ontologyFileName, e);
                throw new RuntimeException(e);
            }
        }

        return ontologyFilePaths;
    }

    /**
     * @return list of path names of files that each specify the properties of a
     *         single DIVIDE query that should be loaded into the DIVIDE engine;
     *         these properties can be read with the {@link DivideQueryConfig} class
     *         (default: empty list)
     */
    public List<String> getDivideQueryPropertiesFiles() {
        String[] queries = config.getStringArray(DIVIDE_QUERIES);
        if (queries == null) {
            return new ArrayList<>();
        } else {
            return Arrays.stream(queries)
                    .filter(Objects::nonNull)
                    .map(path -> !Paths.get(path).isAbsolute() ?
                            Paths.get(configFileDirectory, path).toString() : path)
                    .collect(Collectors.toList());
        }
    }

    /**
     * @return list of path names of files that each specify the properties of a
     *         single DIVIDE query that should be loaded into the DIVIDE engine;
     *         the specification is based on a series of 1 or more chained SPARQL queries;
     *         these properties can be read with the {@link DivideQueryAsRspQlOrSparqlConfig}
     *         class (default: empty list)
     */
    public List<String> getDivideQueryAsSparqlPropertiesFiles() {
        String[] queries = config.getStringArray(DIVIDE_QUERIES_AS_SPARQL);
        if (queries == null) {
            return new ArrayList<>();
        } else {
            return Arrays.stream(queries)
                    .filter(Objects::nonNull)
                    .map(path -> !Paths.get(path).isAbsolute() ?
                            Paths.get(configFileDirectory, path).toString() : path)
                    .collect(Collectors.toList());
        }
    }

    /**
     * @return list of path names of files that each specify the properties of a
     *         single DIVIDE query that should be loaded into the DIVIDE engine;
     *         the specification is based on a single RSP-QL query;
     *         these properties can be read with the {@link DivideQueryAsRspQlOrSparqlConfig}
     *         class (default: empty list)
     */
    public List<String> getDivideQueryAsRspQlPropertiesFiles() {
        String[] queries = config.getStringArray(DIVIDE_QUERIES_AS_RSP_QL);
        if (queries == null) {
            return new ArrayList<>();
        } else {
            return Arrays.stream(queries)
                    .filter(Objects::nonNull)
                    .map(path -> !Paths.get(path).isAbsolute() ?
                            Paths.get(configFileDirectory, path).toString() : path)
                    .collect(Collectors.toList());
        }
    }

}
