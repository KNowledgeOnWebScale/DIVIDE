package be.ugent.idlab.divide.core.query.parser;

import java.util.Set;

public class RspQlQueryBody {

    private final String queryBody;
    private final Set<String> unboundVariables;

    private final QueryForm queryForm;
    private final String resultPart;
    private final String wherePart;

    public RspQlQueryBody(String queryBody,
                          Set<String> unboundVariables,
                          QueryForm queryForm,
                          String resultPart,
                          String wherePart) {
        this.queryBody = queryBody;
        this.unboundVariables = unboundVariables;
        this.queryForm = queryForm;
        this.resultPart = resultPart;
        this.wherePart = wherePart;
    }

    public String getQueryBody() {
        return queryBody;
    }

    public Set<String> getUnboundVariables() {
        return unboundVariables;
    }

    public QueryForm getQueryForm() {
        return queryForm;
    }

    public String getResultPart() {
        return resultPart;
    }

    public String getWherePart() {
        return wherePart;
    }

}
