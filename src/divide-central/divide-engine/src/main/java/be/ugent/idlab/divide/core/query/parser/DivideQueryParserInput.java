package be.ugent.idlab.divide.core.query.parser;

import be.ugent.idlab.util.io.IOUtilities;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class DivideQueryParserInput {

    private InputQueryLanguage inputQueryLanguage;
    private final List<StreamWindow> streamWindows;
    private String streamQuery;
    private List<String> intermediateQueries;
    private String finalQuery;
    private String solutionModifier;
    private Map<String,String> streamToFinalQueryVariableMapping;

    public DivideQueryParserInput(InputQueryLanguage inputQueryLanguage,
                                  List<StreamWindow> streamWindows,
                                  String streamQuery,
                                  List<String> intermediateQueries,
                                  String finalQuery,
                                  String solutionModifier,
                                  Map<String,String> streamToFinalQueryVariableMapping) {
        this.inputQueryLanguage = inputQueryLanguage;
        this.streamWindows = streamWindows;
        this.streamQuery = streamQuery;
        this.intermediateQueries = intermediateQueries;
        this.finalQuery = finalQuery;
        this.solutionModifier = solutionModifier;
        this.streamToFinalQueryVariableMapping = streamToFinalQueryVariableMapping;
    }

    public void validate() throws InvalidDivideQueryParserInputException {
        // input query language should be defined, otherwise correctly parsing is impossible
        if (inputQueryLanguage == null) {
            throw new InvalidDivideQueryParserInputException(
                    "Input query language is not specified");
        }

        if (inputQueryLanguage == InputQueryLanguage.RSP_QL) {
            // for RSP-QL queries, stream windows are already present in the query itself
            // => they should only be defined as a separate entry in the input if the input
            //    defines any default values for the window parameter variables
            if (streamWindows != null && !streamWindows.isEmpty()) {
                if (!streamWindows.stream().allMatch(streamWindow ->
                        streamWindow.getStreamIri() != null &&
                                streamWindow.getDefaultWindowParameterValues() != null &&
                                !streamWindow.getDefaultWindowParameterValues().isEmpty())) {
                    throw new InvalidDivideQueryParserInputException(
                            "Stream windows should only be specified for an RSP-QL query if they " +
                                    "contain the stream IRI and a non-empty list of default window " +
                                    "parameter values - otherwise you should only define them in " +
                                    "the RSP-QL stream query");
                }
            }
        } else if (inputQueryLanguage == InputQueryLanguage.SPARQL) {
            // for SPARQL queries, no window parameters are present yet in the main stream
            // SPARQL query (only an IRI)
            // => stream windows should be specified explicitly as a separate entry in the
            //    input, to ensure that stream IRIs can be mapped on the correct window parameters
            if (streamWindows == null || streamWindows.isEmpty()) {
                throw new InvalidDivideQueryParserInputException(
                        "No names & window parameters specified of the stream graph IRI(s)");
            }

            // there may not be any stream window which is not fully specified
            if (!streamWindows.stream().allMatch(StreamWindow::isValid)) {
                throw new InvalidDivideQueryParserInputException(
                        "Some of the defined stream windows are incomplete or invalid");
            }
        }

        // stream query should always be present (both for SPARQL & RSP-QL case)
        if (streamQuery == null || streamQuery.trim().isEmpty()) {
            throw new InvalidDivideQueryParserInputException(
                    "No stream query specified");
        }

        // for an RSP-QL query, no additional queries can be specified anymore
        // except for the stream query => these inputs should be empty
        if (inputQueryLanguage == InputQueryLanguage.RSP_QL && (
                (intermediateQueries != null && !intermediateQueries.isEmpty()) ||
                        (finalQuery != null && !finalQuery.trim().isEmpty()))) {
            throw new InvalidDivideQueryParserInputException(
                    "Final and/or intermediate queries are specified, which is not " +
                            "possible if the input query language is RSP-QL");
        }

        // not any of the intermediate queries can be null
        if (intermediateQueries != null &&
                intermediateQueries.stream().anyMatch(s -> s == null || s.isEmpty())) {
            throw new InvalidDivideQueryParserInputException(
                    "Some of the intermediate queries are invalid or empty");
        }

        // a variable mapping between a stream and final query can only be provided
        // if a final query is present (= only possible if input language is SPARQL)
        if ((streamToFinalQueryVariableMapping != null &&
                !streamToFinalQueryVariableMapping.isEmpty()) &&
                (inputQueryLanguage != InputQueryLanguage.SPARQL ||
                        finalQuery == null || finalQuery.trim().isEmpty())) {
            throw new InvalidDivideQueryParserInputException(
                    "A variable mapping from stream to final query can only be provided if the " +
                            "input query language is SPARQL and if a final query is specified");
        }
    }

    public void preprocess() {
        // all queries are preprocessed to ensure correct parsing
        this.streamQuery = preprocessQuery(this.streamQuery);
        if (this.intermediateQueries != null) {
            this.intermediateQueries = this.intermediateQueries
                    .stream()
                    .filter(Objects::nonNull)
                    .map(this::preprocessQuery)
                    .collect(Collectors.toList());
        }
        if (this.finalQuery != null) {
            this.finalQuery = preprocessQuery(this.finalQuery);
        }
        if (this.solutionModifier != null && !this.solutionModifier.trim().isEmpty()) {
            this.solutionModifier = preprocessQuery(solutionModifier) + " ";
        } else {
            this.solutionModifier = "";
        }

        // it is ensured a mapping is always available, possibly empty
        if (this.streamToFinalQueryVariableMapping == null) {
            this.streamToFinalQueryVariableMapping = new HashMap<>();
        }
    }

    private String preprocessQuery(String query) {
        return IOUtilities.removeWhiteSpace(query).replace("\r", "").trim();
    }

    public InputQueryLanguage getInputQueryLanguage() {
        return inputQueryLanguage;
    }

    public void setInputQueryLanguage(InputQueryLanguage inputQueryLanguage) {
        this.inputQueryLanguage = inputQueryLanguage;
    }

    public List<StreamWindow> getStreamWindows() {
        return streamWindows;
    }

    public String getStreamQuery() {
        return streamQuery;
    }

    public List<String> getIntermediateQueries() {
        return intermediateQueries;
    }

    public String getFinalQuery() {
        return finalQuery;
    }

    public String getSolutionModifier() {
        return solutionModifier;
    }

    public Map<String, String> getStreamToFinalQueryVariableMapping() {
        return streamToFinalQueryVariableMapping;
    }

    @Override
    public String toString() {
        return "DivideQueryParserInput{" +
                "inputQueryLanguage=" + inputQueryLanguage +
                ", streamWindows=" + streamWindows +
                ", streamQuery='" + streamQuery + '\'' +
                ", intermediateQueries=" + intermediateQueries +
                ", finalQuery='" + finalQuery + '\'' +
                ", solutionModifier='" + solutionModifier + '\'' +
                ", streamToFinalQueryVariableMapping=" + streamToFinalQueryVariableMapping +
                '}';
    }

}
