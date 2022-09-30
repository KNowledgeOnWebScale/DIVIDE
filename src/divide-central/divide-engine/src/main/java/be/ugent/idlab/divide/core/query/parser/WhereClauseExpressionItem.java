package be.ugent.idlab.divide.core.query.parser;

class WhereClauseExpressionItem extends WhereClauseItem {

    private final String expression;

    WhereClauseExpressionItem(String expression) {
        super(WhereClauseItemType.EXPRESSION);
        this.expression = expression;
    }

    String getExpression() {
        return expression;
    }

    @Override
    String getClause() {
        return expression;
    }

    @Override
    public String toString() {
        return "WhereClauseExpressionItem{" +
                "expression='" + expression + '\'' +
                '}';
    }

}
