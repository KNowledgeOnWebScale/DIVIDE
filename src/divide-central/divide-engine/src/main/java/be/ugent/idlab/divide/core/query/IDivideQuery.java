package be.ugent.idlab.divide.core.query;

import be.ugent.idlab.divide.core.context.ContextEnrichment;

/**
 * Representation of a generic query within DIVIDE, of which specific initialized
 * query instances can be derived.
 * It has a name, a query pattern (which needs to be substituted),
 * a sensor query rule used for the query derivation, and a goal used for the query derivation.
 */
public interface IDivideQuery {

    String getName();

    String getQueryPattern();

    String getSensorQueryRule();

    String getGoal();

    ContextEnrichment getContextEnrichment();

    void removeContextEnrichment();

}
