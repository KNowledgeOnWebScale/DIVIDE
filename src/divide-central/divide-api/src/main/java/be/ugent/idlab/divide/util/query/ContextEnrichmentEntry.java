package be.ugent.idlab.divide.util.query;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public class ContextEnrichmentEntry {

    private boolean doReasoning;
    private boolean executeOnOntologyTriples;
    private List<String> queries;

    public ContextEnrichmentEntry() {
        this.doReasoning = true;
        this.executeOnOntologyTriples = true;
        this.queries = new ArrayList<>();
    }

    public boolean doReasoning() {
        return doReasoning;
    }

    public void setDoReasoning(boolean doReasoning) {
        this.doReasoning = doReasoning;
    }

    public boolean executeOnOntologyTriples() {
        return executeOnOntologyTriples;
    }

    public void setExecuteOnOntologyTriples(boolean executeOnOntologyTriples) {
        this.executeOnOntologyTriples = executeOnOntologyTriples;
    }

    public List<String> getQueries() {
        return queries;
    }

    public void setQueries(List<String> queries) {
        this.queries = queries;
    }

}
