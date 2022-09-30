package be.ugent.idlab.divide.rsp.query;

import be.ugent.idlab.divide.core.query.IDivideQuery;

public class RspQueryFactory {

    /**
     * Creates and returns a new RSP query with the given parameters.
     *
     * @param queryName name of the new RSP query
     * @param queryBody body of the new RSP query
     * @param rspQLQueryBody body of the new RSP query in RSP-QL format
     * @param divideQuery the DIVIDE query that was instantiated into the new RSP query
     * @return newly created RSP query
     */
    public static IRspQuery createInstance(String queryName,
                                           String queryBody,
                                           String rspQLQueryBody,
                                           IDivideQuery divideQuery) {
        return new RspQuery(queryName, queryBody, rspQLQueryBody, divideQuery);
    }

}
