package be.ugent.idlab.divide.api.representation.query;

import be.ugent.idlab.divide.core.context.ContextEnrichingQuery;
import be.ugent.idlab.divide.core.context.ContextEnrichment;

import java.util.List;
import java.util.stream.Collectors;

@SuppressWarnings({"FieldCanBeLocal", "unused", "WeakerAccess"})
public class ContextEnrichmentRepresentation {

    private final boolean doReasoning;
    private final boolean executeOnOntologyTriples;
    private final List<String> queries;

    public ContextEnrichmentRepresentation(ContextEnrichment contextEnrichment) {
        switch(contextEnrichment.getMode()) {
            case EXECUTE_ON_CONTEXT_WITHOUT_REASONING:
                this.doReasoning = false;
                this.executeOnOntologyTriples = false;

                break;
            case EXECUTE_ON_CONTEXT_WITH_REASONING:
                this.doReasoning = true;
                this.executeOnOntologyTriples = false;

                break;
            case EXECUTE_ON_CONTEXT_AND_ONTOLOGY_WITHOUT_REASONING:
                this.doReasoning = false;
                this.executeOnOntologyTriples = true;

                break;
            case EXECUTE_ON_CONTEXT_AND_ONTOLOGY_WITH_REASONING:
            default:
                this.doReasoning = true;
                this.executeOnOntologyTriples = true;
        }

        this.queries = contextEnrichment.getQueries().stream()
                .map(ContextEnrichingQuery::getQuery)
                .collect(Collectors.toList());
    }

}
