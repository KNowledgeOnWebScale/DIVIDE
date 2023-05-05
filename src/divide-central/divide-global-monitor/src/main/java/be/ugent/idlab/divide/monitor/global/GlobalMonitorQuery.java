package be.ugent.idlab.divide.monitor.global;

public class GlobalMonitorQuery {

    private final String name;
    private final String body;

    public GlobalMonitorQuery(String name, String body) {
        this.name = name;
        this.body = body;
    }

    public String getName() {
        return name;
    }

    public String getBody() {
        return body;
    }

    @Override
    public String toString() {
        return "GlobalMonitorQuery{" +
                "name='" + name + '\'' +
                ", body='" + body + '\'' +
                '}';
    }

}
