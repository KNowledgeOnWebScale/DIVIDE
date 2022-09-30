package be.ugent.idlab.divide.configuration.legacy;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@SuppressWarnings("unused")
public class DivideVariableMappingConfig {

    private final Configuration config;

    public DivideVariableMappingConfig(String propertiesFilePath) throws ConfigurationException {
        config = new PropertiesConfiguration(propertiesFilePath);
    }

    public Map<String, String> getVariableMapping() throws ConfigurationException {
        Map<String, String> mapping = new HashMap<>();
        Iterator<String> it = config.getKeys();
        while (it.hasNext()) {
            String key = it.next();
            String value = config.getString(key, null);
            if (value == null) {
                throw new ConfigurationException(
                        "Variable mapping file can only contain string values");
            }
            mapping.put(key, value);
        }
        return mapping;
    }

}
