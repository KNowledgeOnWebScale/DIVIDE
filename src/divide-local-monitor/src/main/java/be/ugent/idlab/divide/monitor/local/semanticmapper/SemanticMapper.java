package be.ugent.idlab.divide.monitor.local.semanticmapper;

import be.ugent.idlab.divide.monitor.local.util.JarResourceManager;
import be.ugent.idlab.util.io.IOUtilities;
import be.ugent.idlab.util.process.ProcessException;
import be.ugent.idlab.util.process.ProcessUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Paths;

public class SemanticMapper {

    private static final Logger LOGGER = LoggerFactory.getLogger(SemanticMapper.class.getName());

    private static SemanticMapper instance;

    public static SemanticMapper getInstance() {
        initialize();
        return instance;
    }

    public static void initialize() {
        if (instance == null) {
            instance = new SemanticMapper();
        }
    }

    // name and start-up call for Python semantic mapper script
    private static final String SCRIPT_EXECUTOR = "python3";
    private static final String SCRIPT_NAME = "semantic-mapper.py";

    // actual location of script on local file system
    private final String scriptPath;

    private SemanticMapper() {
        // create an actual file on the file system from the script's resource file
        LOGGER.info("Copying semantic mapper Python script resource from JAR to dedicated file {}", SCRIPT_NAME);
        try {
            this.scriptPath = JarResourceManager.getInstance().copyResourceToFile(Paths.get(SCRIPT_NAME));
        } catch (IOException e) {
            throw new RuntimeException("Semantic mapper Python script could not be copied " +
                    "from the JAR resources to a dedicated file on the file system", e);
        }
    }

    public String mapJsonToRdf(String json,
                               String componentId,
                               String deviceId) {
        try {
            LOGGER.info("Mapping the following monitoring output to RDF: {}", json);
            return IOUtilities.removeWhiteSpace(ProcessUtilities.executeProcess(
                    new String[]{SCRIPT_EXECUTOR, this.scriptPath, componentId, deviceId, json}));
        } catch (IOException | ProcessException e) {
            throw new RuntimeException(e);
        }
    }

}
