package be.ugent.idlab.divide.configuration.legacy;

import be.ugent.idlab.divide.core.query.parser.StreamWindow;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Configuration of a single DIVIDE query that should be added to the DIVIDE
 * engine upon start-up.
 */
@SuppressWarnings("unused")
public class DivideQueryAsRspQlOrSparqlConfig {

    private static final Pattern STREAM_WINDOW_PATTERN = Pattern.compile(
            "(<[^<>]+>)<([^<>]+)><([^<>]+)>");

    private static final String STREAM_GRAPH_NAMES = "stream-graph-names";
    private static final String STREAM_QUERY = "stream-query";
    private static final String INTERMEDIATE_QUERIES = "intermediate-queries";
    private static final String FINAL_QUERY = "final-query";
    private static final String SOLUTION_MODIFIER = "solution-modifier";
    private static final String STREAM_TO_FINAL_QUERY_VARIABLE_MAPPING = "stream-to-final-query-variable-mapping";
    private static final String CONTEXT_ENRICHMENT = "context-enrichment";

    protected final Configuration config;
    private final String queryName;
    private final String configFileDirectory;

    private DivideQueryAsRspQlOrSparqlConfig(String propertiesFilePath) throws ConfigurationException {
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
    public static DivideQueryAsRspQlOrSparqlConfig getInstance(String propertiesFile)
            throws ConfigurationException {
        return new DivideQueryAsRspQlOrSparqlConfig(propertiesFile);
    }

    /**
     * @return the name of the DIVIDE query,
     *         which equals based on the name of the properties file
     */
    public String getQueryName() {
        return queryName;
    }

    public List<StreamWindow> getStreamGraphNames() {
        String[] streamWindowStrings = config.getStringArray(STREAM_GRAPH_NAMES);
        List<StreamWindow> result = new ArrayList<>();
        if (streamWindowStrings != null) {
            for (String streamWindowString : streamWindowStrings) {
                Matcher m = STREAM_WINDOW_PATTERN.matcher(streamWindowString);
                if (m.find()) {
                    result.add(new StreamWindow(m.group(1), String.format("%s %s", m.group(2), m.group(3))));
                } else {
                    // invalid entry, so null returned
                    return null;
                }
            }
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

    public String getStreamToFinalQueryVariableMappingFilePath() {
        String path = config.getString(STREAM_TO_FINAL_QUERY_VARIABLE_MAPPING, "");
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
