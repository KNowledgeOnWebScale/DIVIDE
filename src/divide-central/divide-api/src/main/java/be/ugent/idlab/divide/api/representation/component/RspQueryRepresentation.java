package be.ugent.idlab.divide.api.representation.component;

import be.ugent.idlab.divide.rsp.query.IRspQuery;

@SuppressWarnings({"FieldCanBeLocal", "unused", "WeakerAccess"})
public class RspQueryRepresentation {

    private final String queryName;
    private final String queryBody;

    public RspQueryRepresentation(IRspQuery query) {
        this.queryName = query.getQueryName();
        this.queryBody = query.getQueryBody();
    }

}
