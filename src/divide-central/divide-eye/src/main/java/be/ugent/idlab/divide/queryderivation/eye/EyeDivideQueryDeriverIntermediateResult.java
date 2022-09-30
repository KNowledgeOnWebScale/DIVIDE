package be.ugent.idlab.divide.queryderivation.eye;

public class EyeDivideQueryDeriverIntermediateResult {

    private final String queriesAfterInputVariableSubstitutionFilePath;
    private final String queriesAfterDynamicWindowParameterSubstitutionFilePath;

    public EyeDivideQueryDeriverIntermediateResult(
            String queriesAfterInputVariableSubstitutionFilePath,
            String queriesAfterDynamicWindowParameterSubstitutionFilePath) {
        this.queriesAfterInputVariableSubstitutionFilePath =
                queriesAfterInputVariableSubstitutionFilePath;
        this.queriesAfterDynamicWindowParameterSubstitutionFilePath =
                queriesAfterDynamicWindowParameterSubstitutionFilePath;
    }

    public String getQueriesAfterInputVariableSubstitutionFilePath() {
        return queriesAfterInputVariableSubstitutionFilePath;
    }

    public String getQueriesAfterDynamicWindowParameterSubstitutionFilePath() {
        return queriesAfterDynamicWindowParameterSubstitutionFilePath;
    }

    @Override
    public String toString() {
        return "EyeDivideQueryDeriverIntermediateResult{" +
                "queriesAfterInputVariableSubstitutionFilePath='" +
                queriesAfterInputVariableSubstitutionFilePath + '\'' +
                ", queriesAfterDynamicWindowParameterSubstitutionFilePath='" +
                queriesAfterDynamicWindowParameterSubstitutionFilePath + '\'' +
                '}';
    }

}
