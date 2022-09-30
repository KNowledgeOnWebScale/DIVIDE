package be.ugent.idlab.divide.core.query;

import be.ugent.idlab.divide.core.context.ContextEnrichment;

public class DivideQueryFactory {

    /**
     * Create a new DIVIDE query with the given parameters.
     *
     * @param queryName name of the DIVIDE query
     * @param queryPattern generic query pattern used during query derivation
     * @param sensorQueryRule sensor query rule used during query derivation
     * @param goal goal used during query derivation
     * @param contextEnrichment the context enrichment to be used at the start
     *                          of the query derivation
     * @return created DIVIDE query
     */
    public static IDivideQuery createInstance(String queryName,
                                              String queryPattern,
                                              String sensorQueryRule,
                                              String goal,
                                              ContextEnrichment contextEnrichment) {
        return new DivideQuery(
                queryName, queryPattern, sensorQueryRule, goal, contextEnrichment);
    }

}
