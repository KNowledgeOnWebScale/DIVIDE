package be.ugent.idlab.divide.core.query.parser;

import java.util.List;

public class ConvertedStreamWindow extends StreamWindow {

    private final List<WindowParameter> windowParameters;

    public ConvertedStreamWindow(String streamIri,
                                 String windowDefinition,
                                 List<WindowParameter> windowParameters) {
        super(streamIri, windowDefinition);

        this.windowParameters = windowParameters;
    }

    public List<WindowParameter> getWindowParameters() {
        return windowParameters;
    }

    @Override
    public String toString() {
        return "ConvertedStreamWindow{" +
                "windowParameters=" + windowParameters +
                ", streamIri='" + streamIri + '\'' +
                ", windowDefinition='" + windowDefinition + '\'' +
                ", defaultWindowParameterValues=" + defaultWindowParameterValues +
                '}';
    }

}
