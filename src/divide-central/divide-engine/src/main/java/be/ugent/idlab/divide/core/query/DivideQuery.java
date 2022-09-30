package be.ugent.idlab.divide.core.query;

import be.ugent.idlab.divide.core.context.ContextEnrichment;

import java.util.Objects;

class DivideQuery implements IDivideQuery {

    private final String name;
    private final String queryPattern;
    private final String sensorQueryRule;
    private final String goal;
    private ContextEnrichment contextEnrichment;

    DivideQuery(String name,
                String queryPattern,
                String sensorQueryRule,
                String goal,
                ContextEnrichment contextEnrichment) {
        this.name = name;
        this.queryPattern = queryPattern;
        this.sensorQueryRule = sensorQueryRule;
        this.goal = goal;
        this.contextEnrichment = contextEnrichment;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getQueryPattern() {
        return queryPattern;
    }

    @Override
    public String getSensorQueryRule() {
        return sensorQueryRule;
    }

    @Override
    public String getGoal() {
        return goal;
    }

    @Override
    public ContextEnrichment getContextEnrichment() {
        return contextEnrichment;
    }

    @Override
    public void removeContextEnrichment() {
        this.contextEnrichment = new ContextEnrichment();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DivideQuery that = (DivideQuery) o;
        return name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

}
