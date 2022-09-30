package be.ugent.idlab.divide.core.query.parser;

import javax.annotation.Nonnull;

public class WindowParameter {

    enum WindowParameterType {
        XSD_DURATION,
        TIME_SECONDS,
        TIME_MINUTES,
        TIME_HOURS
    }

    private final String variable;
    private final String value;
    private final WindowParameterType type;
    private final boolean isValueSubstitutionVariable;

    public WindowParameter(String variable,
                           String value,
                           WindowParameterType type,
                           boolean isValueSubstitutionVariable) {
        this.variable = variable;
        this.value = value;
        this.type = type;
        this.isValueSubstitutionVariable = isValueSubstitutionVariable;
    }

    @Nonnull
    public String getVariable() {
        return variable;
    }

    public String getValue() {
        return value;
    }

    public WindowParameterType getType() {
        return type;
    }

    public boolean isValueSubstitutionVariable() {
        return isValueSubstitutionVariable;
    }

    @Override
    public String toString() {
        return "WindowParameter{" +
                "variable='" + variable + '\'' +
                ", value='" + value + '\'' +
                ", type=" + type +
                ", isValueSubstitutionVariable=" + isValueSubstitutionVariable +
                '}';
    }

}
