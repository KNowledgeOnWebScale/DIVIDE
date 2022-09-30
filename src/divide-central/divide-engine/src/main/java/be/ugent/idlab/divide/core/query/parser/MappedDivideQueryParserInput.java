package be.ugent.idlab.divide.core.query.parser;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MappedDivideQueryParserInput extends DivideQueryParserInput {

    private final Map<String, String> finalQueryVariableMapping;

    MappedDivideQueryParserInput(InputQueryLanguage inputQueryLanguage,
                                 List<StreamWindow> streamWindows,
                                 String streamQuery,
                                 List<String> intermediateQueries,
                                 String finalQuery,
                                 String solutionModifier,
                                 Map<String, String> finalQueryVariableMapping) {
        super(inputQueryLanguage, streamWindows, streamQuery, intermediateQueries,
                finalQuery, solutionModifier, new HashMap<>());

        this.finalQueryVariableMapping = new HashMap<>();
        for (Map.Entry<String, String> entry : finalQueryVariableMapping.entrySet()) {
            this.finalQueryVariableMapping.put(entry.getValue(), entry.getKey());
        }
    }

    MappedDivideQueryParserInput(DivideQueryParserInput input) {
        super(input.getInputQueryLanguage(),
                input.getStreamWindows(),
                input.getStreamQuery(),
                input.getIntermediateQueries(),
                input.getFinalQuery(),
                input.getSolutionModifier(),
                input.getStreamToFinalQueryVariableMapping());

        this.finalQueryVariableMapping = new HashMap<>();
    }

    public Map<String, String> getFinalQueryVariableMapping() {
        return finalQueryVariableMapping;
    }

}
