package be.ugent.idlab.divide.core.query.parser;

import java.util.HashMap;
import java.util.Map;

public class StreamWindow {

    protected String streamIri;
    protected String windowDefinition;
    protected Map<String, String> defaultWindowParameterValues;

    public StreamWindow(String streamIri,
                        String windowDefinition,
                        Map<String, String> defaultWindowParameterValues) {
        if (streamIri != null) {
            streamIri = streamIri.trim();
        }
        this.streamIri = streamIri;
        this.windowDefinition = windowDefinition;
        this.defaultWindowParameterValues = defaultWindowParameterValues;
    }

    public StreamWindow(String streamIri,
                        String windowDefinition) {
        this.streamIri = streamIri;
        this.windowDefinition = windowDefinition;
        this.defaultWindowParameterValues = new HashMap<>();
    }

    public String getStreamIri() {
        return streamIri;
    }

    public String getWindowDefinition() {
        return windowDefinition;
    }

    public Map<String, String> getDefaultWindowParameterValues() {
        return defaultWindowParameterValues;
    }

    public void setDefaultWindowParameterValues(Map<String, String> defaultWindowParameterValues) {
        this.defaultWindowParameterValues = defaultWindowParameterValues;
    }

    boolean isValid() {
        return streamIri != null && windowDefinition != null;
    }

    @Override
    public String toString() {
        return "StreamWindow{" +
                "streamIri='" + streamIri + '\'' +
                ", windowDefinition='" + windowDefinition + '\'' +
                ", defaultWindowParameterValues=" + defaultWindowParameterValues +
                '}';
    }

}
