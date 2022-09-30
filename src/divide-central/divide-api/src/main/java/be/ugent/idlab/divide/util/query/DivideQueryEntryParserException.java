package be.ugent.idlab.divide.util.query;

@SuppressWarnings("unused")
public class DivideQueryEntryParserException extends Exception {

    public DivideQueryEntryParserException(String description, Exception base) {
        super(description, base);
    }

    public DivideQueryEntryParserException(String description) {
        super(description);
    }

    public DivideQueryEntryParserException(Exception base) {
        super(base);
    }

}
