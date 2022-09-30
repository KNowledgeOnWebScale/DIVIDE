package be.ugent.idlab.divide.core.query.parser;

import java.util.List;

class ParsedStreamQueryWhereClause {

    private final String contextPart;
    private final List<WhereClauseItem> streamItems;

    ParsedStreamQueryWhereClause(String contextPart,
                                 List<WhereClauseItem> streamItems) {
        this.contextPart = contextPart;
        this.streamItems = streamItems;
    }

    String getContextPart() {
        return contextPart;
    }

    List<WhereClauseItem> getStreamItems() {
        return streamItems;
    }

    @Override
    public String toString() {
        return "ParsedStreamQueryWhereClause{\n" +
                "contextPart='" + contextPart + '\'' +
                ",\nstreamItems=" + streamItems +
                "\n}";
    }

}
