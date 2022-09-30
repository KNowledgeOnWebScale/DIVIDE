package be.ugent.idlab.divide.configuration;

import be.ugent.idlab.divide.configuration.util.CustomJsonConfiguration;
import org.apache.commons.configuration2.JSONConfiguration;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Configuration of a single DIVIDE query that should be added to the DIVIDE
 * engine upon start-up.
 */
public class DivideQueryConfig implements IDivideQueryConfig {

    private static final String QUERY_PATTERN = "queryPattern";
    private static final String SENSOR_QUERY_RULE = "sensorQueryRule";
    private static final String GOAL = "goal";

    private static final String CONTEXT_ENRICHMENT_DO_REASONING = "contextEnrichment.doReasoning";
    private static final String CONTEXT_ENRICHMENT_EXECUTE_ON_ONTOLOGY_TRIPLES =
            "contextEnrichment.executeOnOntologyTriples";
    private static final String CONTEXT_ENRICHMENT_QUERIES = "contextEnrichment.queries";

    private final JSONConfiguration config;
    private final String queryName;
    private final String configFileDirectory;

    private DivideQueryConfig(String propertiesFilePath)
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
    public static DivideQueryConfig getInstance(String propertiesFile)
            throws ConfigurationException, FileNotFoundException {
        return new DivideQueryConfig(propertiesFile);
    }

    /**
     * @return the name of the DIVIDE query,
     *         which equals based on the name of the properties file
     */
    public String getQueryName() {
        return queryName;
    }

    /**
     * @return path to query pattern file specified in DIVIDE query config file,
     *         or null if not specified
     */
    public String getQueryPatternFilePath() {
        String path = config.getString(QUERY_PATTERN, "");
        if (!path.isEmpty() && !Paths.get(path).isAbsolute()) {
            path = Paths.get(configFileDirectory, path).toString();
        }
        return path;
    }

    /**
     * @return path to sensor query rule file specified in DIVIDE query config file,
     *         or null if not specified
     */
    public String getSensorQueryRuleFilePath() {
        String path = config.getString(SENSOR_QUERY_RULE, "");
        if (!path.isEmpty() && !Paths.get(path).isAbsolute()) {
            path = Paths.get(configFileDirectory, path).toString();
        }
        return path;
    }

    /**
     * @return path to goal file specified in DIVIDE query config file,
     *         or null if not specified
     */
    public String getGoalFilePath() {
        String path = config.getString(GOAL, "");
        if (!path.isEmpty() && !Paths.get(path).isAbsolute()) {
            path = Paths.get(configFileDirectory, path).toString();
        }
        return path;
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
