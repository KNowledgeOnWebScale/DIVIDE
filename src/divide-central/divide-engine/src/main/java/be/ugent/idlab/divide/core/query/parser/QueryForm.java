package be.ugent.idlab.divide.core.query.parser;

public enum QueryForm {

    CONSTRUCT, SELECT, ASK, DESCRIBE;

    static QueryForm fromString(String name) {
        for (QueryForm queryForm : QueryForm.values()) {
            if (queryForm.name().equalsIgnoreCase(name)) {
                return queryForm;
            }
        }
        return null;
    }

}