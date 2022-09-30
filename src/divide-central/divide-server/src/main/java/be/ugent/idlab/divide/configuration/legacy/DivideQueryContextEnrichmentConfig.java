package be.ugent.idlab.divide.configuration.legacy;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SuppressWarnings("unused")
public class DivideQueryContextEnrichmentConfig {

    private static final String DO_REASONING = "do-reasoning";
    private static final String EXECUTE_ON_ONTOLOGY_TRIPLES = "execute-on-ontology-triples";
    private static final String QUERIES = "queries";

    protected final Configuration config;
    private final String configFileDirectory;

    public DivideQueryContextEnrichmentConfig(String propertiesFilePath) throws ConfigurationException {
        config = new PropertiesConfiguration(propertiesFilePath);
        configFileDirectory = new File(propertiesFilePath)
                .getAbsoluteFile().getParentFile().getAbsolutePath();
    }

    public boolean executeOnOntologyTriples() {
        return config.getBoolean(EXECUTE_ON_ONTOLOGY_TRIPLES, true);
    }

    public boolean doReasoning() {
        return config.getBoolean(DO_REASONING, true);
    }

    public List<String> getQueryFilePaths() {
        String[] queries = config.getStringArray(QUERIES);
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
