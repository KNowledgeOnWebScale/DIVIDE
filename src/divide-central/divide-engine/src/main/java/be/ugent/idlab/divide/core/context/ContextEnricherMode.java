package be.ugent.idlab.divide.core.context;

public enum ContextEnricherMode {

    EXECUTE_ON_CONTEXT_WITHOUT_REASONING(false, false),
    EXECUTE_ON_CONTEXT_WITH_REASONING(false, true),
    EXECUTE_ON_CONTEXT_AND_ONTOLOGY_WITHOUT_REASONING(true, false),
    EXECUTE_ON_CONTEXT_AND_ONTOLOGY_WITH_REASONING(true, true);

    private final boolean loadOntology;
    private final boolean performReasoning;

    ContextEnricherMode(boolean loadOntology, boolean performReasoning) {
        this.loadOntology = loadOntology;
        this.performReasoning = performReasoning;
    }

    public boolean loadOntology() {
        return loadOntology;
    }

    public boolean performReasoning() {
        return performReasoning;
    }

}
