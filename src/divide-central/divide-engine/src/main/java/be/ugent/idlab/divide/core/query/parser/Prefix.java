package be.ugent.idlab.divide.core.query.parser;

import java.util.Objects;

public class Prefix {

    private final String name;
    private final String uri;

    Prefix(String name, String uri) {
        this.name = name;
        this.uri = uri;
    }

    public String getName() {
        return name;
    }

    public String getUri() {
        return uri;
    }

    @Override
    public String toString() {
        return "Prefix{" +
                "name='" + name + '\'' +
                ", uri='" + uri + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Prefix prefix = (Prefix) o;
        return name.equals(prefix.name) && uri.equals(prefix.uri);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, uri);
    }

}
