package be.ugent.idlab.divide.core.query.parser;

import java.util.Map;
import java.util.Set;

public class ParsedStreamWindow extends StreamWindow {

    private final Set<String> unboundVariables;

    public ParsedStreamWindow(String streamIri,
                              String windowDefinition,
                              Map<String, String> defaultWindowParameterValues,
                              Set<String> unboundVariables) {
        super(streamIri, windowDefinition, defaultWindowParameterValues);

        this.unboundVariables = unboundVariables;
    }

    public Set<String> getUnboundVariables() {
        return unboundVariables;
    }

    @Override
    public String toString() {
        return "ParsedStreamWindow{" +
                "streamIri='" + streamIri + '\'' +
                ", windowDefinition='" + windowDefinition + '\'' +
                ", unboundVariables=" + unboundVariables +
                ", defaultWindowParameterValues=" + defaultWindowParameterValues +
                '}';
    }

}
