package be.ugent.idlab.divide.core.query.parser;

public class DivideQueryParserOutput {

    private final String queryPattern;
    private final String sensorQueryRule;
    private final String goal;

    private final QueryForm queryForm;

    public DivideQueryParserOutput(String queryPattern,
                                   String sensorQueryRule,
                                   String goal,
                                   QueryForm queryForm) {
        this.queryPattern = queryPattern;
        this.sensorQueryRule = sensorQueryRule;
        this.goal = goal;
        this.queryForm = queryForm;
    }

    public String getQueryPattern() {
        return queryPattern;
    }

    public String getSensorQueryRule() {
        return sensorQueryRule;
    }

    public String getGoal() {
        return goal;
    }

    public QueryForm getQueryForm() {
        return queryForm;
    }

    public boolean isNonEmpty() {
        return queryPattern != null && !queryPattern.isEmpty() &&
                sensorQueryRule != null && !sensorQueryRule.isEmpty() &&
                goal != null && !goal.isEmpty();
    }

}
