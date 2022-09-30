package be.ugent.idlab.divide.core.context;

public class ContextEnrichingQuery {

    private final String name;
    private final String query;

    ContextEnrichingQuery(String name, String query) {
        this.name = name;
        this.query = query;
    }

    ContextEnrichingQuery(int order, String query) {
        this.name = String.format("query-%d", order);
        this.query = query;
    }

    public String getName() {
        return name;
    }

    public String getQuery() {
        return query;
    }

}
