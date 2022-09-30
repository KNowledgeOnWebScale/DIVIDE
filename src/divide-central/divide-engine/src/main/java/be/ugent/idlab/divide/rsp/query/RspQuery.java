package be.ugent.idlab.divide.rsp.query;

import be.ugent.idlab.divide.core.query.IDivideQuery;

import java.util.Objects;

public class RspQuery implements IRspQuery {

    private final String queryName;
    private final String queryBody;
    private final String rspQLQueryBody;
    private final IDivideQuery divideQuery;

    public RspQuery(String queryName,
                    String queryBody,
                    String rspQLQueryBody,
                    IDivideQuery divideQuery) {
        this.queryName = queryName;
        this.queryBody = queryBody;
        this.rspQLQueryBody = rspQLQueryBody;
        this.divideQuery = divideQuery;
    }

    @Override
    public String getQueryName() {
        return queryName;
    }

    @Override
    public String getQueryBody() {
        return queryBody;
    }

    @Override
    public IDivideQuery getOriginalDivideQuery() {
        return divideQuery;
    }

    // IMPORTANT: equality of RSP queries is defined by their RSP-QL body,
    //            and NOT by their name or translated body (which might
    //            contain the name)!
    //            (since body comparison is done to determine whether
    //             a query is already registered on an RSP engine or not)
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RspQuery rspQuery = (RspQuery) o;
        return rspQLQueryBody.equals(rspQuery.rspQLQueryBody);
    }

    @Override
    public int hashCode() {
        return Objects.hash(queryBody);
    }

}
