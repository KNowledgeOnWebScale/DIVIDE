package be.ugent.idlab.divide.util.query;

@SuppressWarnings("unused")
public class DivideQueryEntryInDivideFormat {

    private String queryPattern;
    private String sensorQueryRule;
    private String goal;

    private ContextEnrichmentEntry contextEnrichment;

    public DivideQueryEntryInDivideFormat() {
        // empty on purpose
    }

    public String getQueryPattern() {
        return queryPattern;
    }

    public void setQueryPattern(String queryPattern) {
        this.queryPattern = queryPattern;
    }

    public String getSensorQueryRule() {
        return sensorQueryRule;
    }

    public void setSensorQueryRule(String sensorQueryRule) {
        this.sensorQueryRule = sensorQueryRule;
    }

    public String getGoal() {
        return goal;
    }

    public void setGoal(String goal) {
        this.goal = goal;
    }

    public ContextEnrichmentEntry getContextEnrichment() {
        return contextEnrichment;
    }

    public void setContextEnrichment(ContextEnrichmentEntry contextEnrichmentEntry) {
        this.contextEnrichment = contextEnrichmentEntry;
    }

}
