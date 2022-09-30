package be.ugent.idlab.divide.rsp.query;

import be.ugent.idlab.divide.core.query.IDivideQuery;

/**
 * Representation of an RSP query, which has a name and a body.
 */
public interface IRspQuery {

    /**
     * @return name of RSP query
     */
    String getQueryName();

    /**
     * @return body of RSP query
     */
    String getQueryBody();

    /**
     * @return a reference to the DIVIDE query that was instantiated into this RSP query
     */
    IDivideQuery getOriginalDivideQuery();

}
