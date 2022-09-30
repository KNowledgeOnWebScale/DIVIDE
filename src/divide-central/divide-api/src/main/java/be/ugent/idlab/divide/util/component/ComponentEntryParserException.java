package be.ugent.idlab.divide.util.component;

@SuppressWarnings("unused")
public class ComponentEntryParserException extends Exception {

    public ComponentEntryParserException(String description, Exception base) {
        super(description, base);
    }

    public ComponentEntryParserException(String description) {
        super(description);
    }

    public ComponentEntryParserException(Exception base) {
        super(base);
    }

}
