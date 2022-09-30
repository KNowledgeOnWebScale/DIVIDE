package be.ugent.idlab.divide.core.query.parser;

import java.util.List;

class WhereClause {

    private final List<WhereClauseItem> items;

    WhereClause(List<WhereClauseItem> items) {
        this.items = items;
    }

    List<WhereClauseItem> getItems() {
        return items;
    }

    @Override
    public String toString() {
        return "WhereClause{" +
                "items=" + items +
                '}';
    }

}
