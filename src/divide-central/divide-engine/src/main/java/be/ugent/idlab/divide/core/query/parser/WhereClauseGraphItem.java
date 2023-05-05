package be.ugent.idlab.divide.core.query.parser;

class WhereClauseGraphItem extends WhereClauseItem {

    private final Graph graph;

    WhereClauseGraphItem(Graph graph) {
        super(WhereClauseItemType.GRAPH);
        this.graph = graph;
    }

    Graph getGraph() {
        return graph;
    }

    @Override
    String getClause() {
        return graph.getClause();
    }

    @Override
    public String toString() {
        return "WhereClauseGraphItem{" +
                "graph=" + graph +
                '}';
    }

}
