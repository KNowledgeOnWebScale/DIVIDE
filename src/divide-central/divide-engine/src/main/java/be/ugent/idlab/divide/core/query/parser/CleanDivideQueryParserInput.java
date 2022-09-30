package be.ugent.idlab.divide.core.query.parser;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CleanDivideQueryParserInput extends DivideQueryParserInput {

    private final Map<String, String> variableMapping;
    private final Map<String, String> reverseVariableMapping;
    private Set<String> unboundVariables;
    private Map<String, String> finalQueryVariableMapping;

    CleanDivideQueryParserInput(InputQueryLanguage inputQueryLanguage,
                                List<StreamWindow> streamWindows,
                                String streamQuery,
                                List<String> intermediateQueries,
                                String finalQuery,
                                String solutionModifier,
                                Map<String, String> variableMapping) {
        super(inputQueryLanguage, streamWindows, streamQuery, intermediateQueries,
                finalQuery, solutionModifier, null);

        this.variableMapping = variableMapping;
        this.reverseVariableMapping = new HashMap<>();
        this.variableMapping.forEach((k, v) -> this.reverseVariableMapping.put(v, k));

        this.finalQueryVariableMapping = new HashMap<>();
    }

    CleanDivideQueryParserInput(DivideQueryParserInput input) {
        super(input.getInputQueryLanguage(),
                input.getStreamWindows(),
                input.getStreamQuery(),
                input.getIntermediateQueries(),
                input.getFinalQuery(),
                input.getSolutionModifier(),
                input.getStreamToFinalQueryVariableMapping());

        this.variableMapping = new HashMap<>();
        this.reverseVariableMapping = new HashMap<>();

        this.finalQueryVariableMapping = new HashMap<>();
    }

    Map<String, String> getVariableMapping() {
        return variableMapping;
    }

    public Map<String, String> getReverseVariableMapping() {
        return reverseVariableMapping;
    }

    void setUnboundVariables(Set<String> unboundVariables) {
        this.unboundVariables = unboundVariables;
    }

    Set<String> getUnboundVariables() {
        return unboundVariables;
    }

    public void setFinalQueryVariableMapping(Map<String, String> finalQueryVariableMapping) {
        this.finalQueryVariableMapping = finalQueryVariableMapping;
    }

    public Map<String, String> getFinalQueryVariableMapping() {
        return finalQueryVariableMapping;
    }

}
