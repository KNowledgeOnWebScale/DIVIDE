package be.ugent.idlab.divide.configuration.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.MapType;
import org.apache.commons.configuration2.JSONConfiguration;
import org.apache.commons.configuration2.ex.ConfigurationException;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.Map;

public class CustomJsonConfiguration extends JSONConfiguration {

    private final ObjectMapper mapper = new ObjectMapper();
    private final MapType type;

    public CustomJsonConfiguration(String filePath) throws FileNotFoundException, ConfigurationException {
        this.mapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
        this.type = this.mapper.getTypeFactory().constructMapType(
                Map.class, String.class, Object.class);

        read(new FileReader(filePath));
    }

    @Override
    public void read(Reader in) throws ConfigurationException {
        try {
            Map<String, Object> map = this.mapper.readValue(in, this.type);
            this.load(map);
        } catch (Exception var3) {
            rethrowException(var3);
        }
    }

    @Override
    public void write(Writer out) throws ConfigurationException, IOException {
        this.mapper.writer().writeValue(out,
                this.constructMap(this.getNodeModel().getNodeHandler().getRootNode()));
    }

    @Override
    public void read(InputStream in) throws ConfigurationException {
        try {
            Map<String, Object> map = this.mapper.readValue(in, this.type);
            this.load(map);
        } catch (Exception var3) {
            rethrowException(var3);
        }
    }

    static void rethrowException(Exception e) throws ConfigurationException {
        if (e instanceof ClassCastException) {
            throw new ConfigurationException("Error parsing", e);
        } else {
            throw new ConfigurationException("Unable to load the configuration", e);
        }
    }

}
