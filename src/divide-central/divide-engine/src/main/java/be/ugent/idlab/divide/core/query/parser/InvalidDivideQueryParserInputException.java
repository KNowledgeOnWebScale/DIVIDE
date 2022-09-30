package be.ugent.idlab.divide.core.query.parser;

public class InvalidDivideQueryParserInputException extends Exception {

    public InvalidDivideQueryParserInputException(String description, Exception base) {
        super(description, base);
    }

    public InvalidDivideQueryParserInputException(String description) {
        super(description);
    }

}
