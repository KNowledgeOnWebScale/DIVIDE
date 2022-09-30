package be.ugent.idlab.divide.core.query.parser;

public class SplitSparqlQuery {

    private final String prefixPart;
    private final QueryForm queryForm;
    private final String resultPart;
    private final String fromPart;
    private final String wherePart;
    private final String finalPart;

    SplitSparqlQuery(String prefixPart,
                     QueryForm queryForm,
                     String resultPart,
                     String fromPart,
                     String wherePart,
                     String finalPart) {
        this.prefixPart = prefixPart;
        this.queryForm = queryForm;
        this.resultPart = resultPart;
        this.fromPart = fromPart;
        this.wherePart = wherePart;
        this.finalPart = finalPart;
    }

    public String getPrefixPart() {
        return prefixPart;
    }

    public QueryForm getQueryForm() {
        return queryForm;
    }

    public String getResultPart() {
        return resultPart;
    }

    public String getFromPart() {
        return fromPart;
    }

    public String getWherePart() {
        return wherePart;
    }

    public String getFinalPart() {
        return finalPart;
    }

    @Override
    public String toString() {
        return "SplitSparqlQuery{\n" +
                "prefixPart='" + prefixPart + '\'' +
                ",\nqueryForm=" + queryForm +
                ",\nresultPart='" + resultPart + '\'' +
                ",\nfromPart='" + fromPart + '\'' +
                ",\nwherePart='" + wherePart + '\'' +
                ",\nfinalPart='" + finalPart + '\'' +
                '}';
    }

}
