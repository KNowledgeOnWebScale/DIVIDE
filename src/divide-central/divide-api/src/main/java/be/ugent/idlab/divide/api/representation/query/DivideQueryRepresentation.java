package be.ugent.idlab.divide.api.representation.query;

import be.ugent.idlab.divide.core.query.IDivideQuery;

@SuppressWarnings({"FieldCanBeLocal", "unused"})
public class DivideQueryRepresentation {

    private final String name;
    private final String queryPattern;
    private final String sensorQueryRule;
    private final String goal;
    private final ContextEnrichmentRepresentation contextEnrichment;

    public DivideQueryRepresentation(IDivideQuery divideQuery) {
        this.name = divideQuery.getName();
        this.queryPattern = divideQuery.getQueryPattern();
        this.sensorQueryRule = divideQuery.getSensorQueryRule();
        this.goal = divideQuery.getGoal();
        this.contextEnrichment = new ContextEnrichmentRepresentation(
                divideQuery.getContextEnrichment());
    }

}
