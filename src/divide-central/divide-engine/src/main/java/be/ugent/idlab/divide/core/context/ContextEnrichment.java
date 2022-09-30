package be.ugent.idlab.divide.core.context;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ContextEnrichment {

    private final ContextEnricherMode mode;
    private final List<ContextEnrichingQuery> queries;

    public ContextEnrichment() {
        // default constructor when no context enrichment is available
        this.mode = ContextEnricherMode.EXECUTE_ON_CONTEXT_WITHOUT_REASONING;
        this.queries = new ArrayList<>();
    }

    public ContextEnrichment(boolean doReasoning,
                             boolean executeWithOntologyTriples,
                             List<String> queries) {
        // set list of queries
        this.queries = IntStream.range(0, queries.size())
                .mapToObj(i -> new ContextEnrichingQuery(i, queries.get(i)))
                .filter(query -> query.getQuery() != null && !query.getQuery().trim().isEmpty())
                .collect(Collectors.toList());

        // set correct mode
        if (this.queries.isEmpty()) {
            this.mode = ContextEnricherMode.EXECUTE_ON_CONTEXT_WITHOUT_REASONING;
        } else {
            this.mode = executeWithOntologyTriples ?
                    (doReasoning ? ContextEnricherMode.EXECUTE_ON_CONTEXT_AND_ONTOLOGY_WITH_REASONING :
                            ContextEnricherMode.EXECUTE_ON_CONTEXT_AND_ONTOLOGY_WITHOUT_REASONING) :
                    (doReasoning ? ContextEnricherMode.EXECUTE_ON_CONTEXT_WITH_REASONING :
                            ContextEnricherMode.EXECUTE_ON_CONTEXT_WITHOUT_REASONING);
        }
    }

    public ContextEnricherMode getMode() {
        return mode;
    }

    public List<ContextEnrichingQuery> getQueries() {
        return queries;
    }

    @Override
    public String toString() {
        return "ContextEnrichment{" +
                "mode=" + mode +
                ", queries=" + queries +
                '}';
    }

}
