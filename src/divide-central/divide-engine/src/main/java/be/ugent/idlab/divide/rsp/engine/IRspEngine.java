package be.ugent.idlab.divide.rsp.engine;

import be.ugent.idlab.divide.rsp.RspQueryLanguage;
import be.ugent.idlab.divide.rsp.query.IRspQuery;

import java.util.List;

/**
 * Representation of an RSP engine. It has a query language, registration URL,
 * and a set of registered RSP queries ({@link IRspQuery} instances).
 */
public interface IRspEngine {

    String getId();

    /**
     * @return the query language used by this RSP engine
     */
    RspQueryLanguage getRspQueryLanguage();

    /**
     * @return the base URL of this RSP engine (e.g. used as base URL for constructing
     *         the URL to which queries should be registered to this RSP engine and
     *         unregistered from it)
     */
    String getBaseUrl();

    int getServerPort();

    void setWebSocketStreamUrl(String webSocketStreamUrl);

    String getWebSocketStreamUrl();

    /**
     * @return blueprint of queries that are currently actually registered
     *         at this RSP engine
     */
    List<IRspQuery> getRegisteredQueries();

    /**
     * Updates the list of registered queries at this RSP engine
     * by adding a new query.
     * If the query is already present in the list, nothing happens.
     *
     * @param query query to be added to the list of registered queries
     */
    void addRegisteredQuery(IRspQuery query);

    /**
     * Updates the list of registered queries at this RSP engine
     * by removing a query.
     * If the query is not present in the list, nothing happens.
     *
     * @param query query to be removed to the list of registered queries
     */
    void removeRegisteredQuery(IRspQuery query);

}
