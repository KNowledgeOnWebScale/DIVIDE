package be.ugent.idlab.divide.core.query.parser;

abstract class WhereClauseItem {

    protected final WhereClauseItemType itemType;

    WhereClauseItem(WhereClauseItemType itemType) {
        this.itemType = itemType;
    }

    WhereClauseItemType getItemType() {
        return itemType;
    }

    abstract String getClause();

}
