package be.ugent.idlab.divide.core.query.parser;

class Graph {

    private final String name;
    private final String clause;

    Graph(String name, String clause) {
        this.name = name;
        this.clause = clause;
    }

    String getName() {
        return name;
    }

    String getClause() {
        return clause;
    }

    @Override
    public String toString() {
        return "Graph{" +
                "name='" + name + '\'' +
                ", clause='" + clause + '\'' +
                '}';
    }

}
