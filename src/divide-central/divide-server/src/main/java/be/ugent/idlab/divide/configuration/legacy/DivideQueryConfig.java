package be.ugent.idlab.divide.configuration.legacy;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.nio.file.Paths;

/**
 * Configuration of a single DIVIDE query that should be added to the DIVIDE
 * engine upon start-up.
 */
@SuppressWarnings("unused")
public class DivideQueryConfig {

    private static final String QUERY_PATTERN = "query-pattern";
    private static final String SENSOR_QUERY_RULE = "sensor-query-rule";
    private static final String GOAL = "goal";

    private static final String CONTEXT_ENRICHMENT = "context-enrichment";

    private final Configuration config;
    private final String queryName;
    private final String configFileDirectory;

    private DivideQueryConfig(String propertiesFilePath) throws ConfigurationException {
        config = new PropertiesConfiguration(propertiesFilePath);
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
     * @throws ConfigurationException if the properties file does not exist or is invalid
     */
    public static DivideQueryConfig getInstance(String propertiesFile)
            throws ConfigurationException {
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

    public String getContextEnrichmentFilePath() {
        String path = config.getString(CONTEXT_ENRICHMENT, "");
        if (!path.isEmpty() && !Paths.get(path).isAbsolute()) {
            path = Paths.get(configFileDirectory, path).toString();
        }
        return path;
    }

}
