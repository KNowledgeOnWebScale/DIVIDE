package be.ugent.idlab.divide.configuration.query;

import be.ugent.idlab.divide.configuration.util.CustomJsonConfiguration;
import be.ugent.idlab.divide.core.query.parser.StreamWindow;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.apache.commons.configuration2.JSONConfiguration;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.configuration2.ex.ConfigurationRuntimeException;
import org.apache.commons.configuration2.tree.ImmutableNode;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Configuration of a single DIVIDE query that should be added to the DIVIDE
 * engine upon start-up.
 */
public class DivideQueryAsRspQlOrSparqlConfig implements IDivideQueryConfig {

    private static final String STREAM_WINDOWS = "streamWindows";
    private static final String STREAM_WINDOW_STREAM_IRI = "streamIri";
    private static final String STREAM_WINDOW_WINDOW_DEFINITION = "windowDefinition";
    private static final String STREAM_WINDOW_DEFAULT_WINDOW_PARAMETER_VALUES = "defaultWindowParameterValues";

    private static final String STREAM_QUERY = "streamQuery";
    private static final String INTERMEDIATE_QUERIES = "intermediateQueries";
    private static final String FINAL_QUERY = "finalQuery";
    private static final String SOLUTION_MODIFIER = "solutionModifier";
    private static final String STREAM_TO_FINAL_QUERY_VARIABLE_MAPPING = "streamToFinalQueryVariableMapping";

    private static final String CONTEXT_ENRICHMENT_DO_REASONING = "contextEnrichment.doReasoning";
    private static final String CONTEXT_ENRICHMENT_EXECUTE_ON_ONTOLOGY_TRIPLES =
            "contextEnrichment.executeOnOntologyTriples";
    private static final String CONTEXT_ENRICHMENT_QUERIES = "contextEnrichment.queries";

    protected final JSONConfiguration config;
    private final String queryName;
    private final String configFileDirectory;

    private DivideQueryAsRspQlOrSparqlConfig(String propertiesFilePath)
            throws ConfigurationException, FileNotFoundException {
        config = new CustomJsonConfiguration(propertiesFilePath);
        queryName = FilenameUtils.getBaseName(propertiesFilePath);
        configFileDirectory = new File(propertiesFilePath)
                .getAbsoluteFile().getParentFile().getAbsolutePath();
    }

    /**
     * Creates a DIVIDE query config object based on the given properties file.
     *
     * @param propertiesFile path to query properties file
     * @return an instantiated DIVIDE query config object which can be used to
     *         retrieve the configuration parameters of the DIVIDE query
     * @throws ConfigurationException if the properties file is invalid
     * @throws FileNotFoundException if the properties file does not exist
     */
    public static DivideQueryAsRspQlOrSparqlConfig getInstance(String propertiesFile)
            throws ConfigurationException, FileNotFoundException {
        return new DivideQueryAsRspQlOrSparqlConfig(propertiesFile);
    }

    /**
     * @return the name of the DIVIDE query,
     *         which equals based on the name of the properties file
     */
    public String getQueryName() {
        return queryName;
    }

    public List<StreamWindow> getStreamWindows() throws ConfigurationException {
        List<StreamWindow> result = new ArrayList<>();
        for (HierarchicalConfiguration<ImmutableNode> streamWindowConfig :
                config.configurationsAt(STREAM_WINDOWS)) {
            String streamIri = streamWindowConfig.getString(STREAM_WINDOW_STREAM_IRI);
            if (streamIri != null) {
                streamIri = "<" + streamIri +">";
            }

            String windowDefinition = streamWindowConfig.getString(STREAM_WINDOW_WINDOW_DEFINITION);

            Map<String, String> defaultWindowParameters = new HashMap<>();
            try {
                Configuration variableMappingConfig =
                        streamWindowConfig.configurationAt(STREAM_WINDOW_DEFAULT_WINDOW_PARAMETER_VALUES);
                Iterator<String> it = variableMappingConfig.getKeys();
                while (it.hasNext()) {
                    String key = it.next();
                    String value = variableMappingConfig.getString(key, null);
                    if (value == null) {
                        throw new ConfigurationException(
                                "Default window parameter mapping file can only contain string values");
                    }
                    defaultWindowParameters.put(key, value);
                }
            } catch (ConfigurationRuntimeException e) {
                defaultWindowParameters = new HashMap<>();
            }

            if (streamIri == null) {
                // invalid entry, so null returned
                return null;
            }

            result.add(new StreamWindow(streamIri, windowDefinition, defaultWindowParameters));
        }
        return result;
    }

    public String getStreamQueryFilePath() {
        String path = config.getString(STREAM_QUERY, "");
        if (!path.isEmpty() && !Paths.get(path).isAbsolute()) {
            path = Paths.get(configFileDirectory, path).toString();
        }
        return path;
    }

    public List<String> getIntermediateQueryFilePaths() {
        String[] queries = config.getStringArray(INTERMEDIATE_QUERIES);
        List<String> queryList = queries == null ? new ArrayList<>() : Arrays.asList(queries);
        List<String> result = new ArrayList<>();
        for (String path : queryList) {
            if (path != null && !path.isEmpty() && !Paths.get(path).isAbsolute()) {
                path = Paths.get(configFileDirectory, path).toString();
            }
            result.add(path);
        }
        return result;
    }

    public String getFinalQueryFilePath() {
        String path = config.getString(FINAL_QUERY, "");
        if (!path.isEmpty() && !Paths.get(path).isAbsolute()) {
            path = Paths.get(configFileDirectory, path).toString();
        }
        return path;
    }

    public String getSolutionModifier() {
        return config.getString(SOLUTION_MODIFIER, null);
    }

    public Map<String, String> getStreamToFinalQueryVariableMapping() throws ConfigurationException {
        Map<String, String> mapping = new HashMap<>();
        try {
            Configuration variableMappingConfig =
                    config.configurationAt(STREAM_TO_FINAL_QUERY_VARIABLE_MAPPING);
            Iterator<String> it = variableMappingConfig.getKeys();
            while (it.hasNext()) {
                String key = it.next();
                String value = variableMappingConfig.getString(key, null);
                if (value == null) {
                    throw new ConfigurationException(
                            "Variable mapping file can only contain string values");
                }
                mapping.put(key, value);
            }
            return mapping;
        } catch (ConfigurationRuntimeException e) {
            return new HashMap<>();
        }
    }

    @Override
    public boolean getContextEnrichmentDoReasoning() {
        return config.getBoolean(CONTEXT_ENRICHMENT_DO_REASONING, true);
    }

    @Override
    public boolean getContextEnrichmentExecuteOnOntologyTriples() {
        return config.getBoolean(CONTEXT_ENRICHMENT_EXECUTE_ON_ONTOLOGY_TRIPLES, true);
    }

    @Override
    public List<String> getContextEnrichmentQueryFilePaths() {
        String[] queries = config.getStringArray(CONTEXT_ENRICHMENT_QUERIES);
        List<String> queryList = queries == null ? new ArrayList<>() : Arrays.asList(queries);
        List<String> result = new ArrayList<>();
        for (String path : queryList) {
            if (path != null && !path.isEmpty() && !Paths.get(path).isAbsolute()) {
                path = Paths.get(configFileDirectory, path).toString();
            }
            result.add(path);
        }
        return result;
    }

}
