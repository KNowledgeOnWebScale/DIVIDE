package be.ugent.idlab.divide.core.context;

public class ContextEnricherFactory {

    /**
     * Create and return a new DIVIDE context enricher.
     *
     * @return newly created DIVIDE context enricher
     */
    public static synchronized IContextEnricher createInstance(ContextEnrichment contextEnrichment,
                                                               String componentId) {
        // only create a context enricher with actual logic, if context enriching queries
        // are defined in the given context enrichment
        if (contextEnrichment == null ||
                contextEnrichment.getQueries() == null ||
                contextEnrichment.getQueries().isEmpty()) {
            return new DummyContextEnricher();
        } else {
            return new ContextEnricher(
                    contextEnrichment.getQueries(),
                    contextEnrichment.getMode(),
                    componentId);
        }
    }

}
