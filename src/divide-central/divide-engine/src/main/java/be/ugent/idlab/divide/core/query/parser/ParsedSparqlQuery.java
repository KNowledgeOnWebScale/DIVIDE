package be.ugent.idlab.divide.core.query.parser;

import java.util.Set;

public class ParsedSparqlQuery {

    private final SplitSparqlQuery splitSparqlQuery;
    private final Set<Prefix> prefixes;

    public ParsedSparqlQuery(SplitSparqlQuery splitSparqlQuery,
                             Set<Prefix> prefixes) {
        this.splitSparqlQuery = splitSparqlQuery;
        this.prefixes = prefixes;
    }

    public SplitSparqlQuery getSplitSparqlQuery() {
        return splitSparqlQuery;
    }

    public Set<Prefix> getPrefixes() {
        return prefixes;
    }

    @Override
    public String toString() {
        return "ParsedSparqlQuery{" +
                "splitSparqlQuery=" + splitSparqlQuery +
                ", prefixes=" + prefixes +
                '}';
    }

}
