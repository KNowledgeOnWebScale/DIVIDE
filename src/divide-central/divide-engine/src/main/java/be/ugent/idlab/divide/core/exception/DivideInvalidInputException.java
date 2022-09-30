package be.ugent.idlab.divide.core.exception;

/**
 * Exception thrown when an error occurs because the input provided to a DIVIDE
 * object (from the outside) is invalid.
 */
@SuppressWarnings("unused")
public class DivideInvalidInputException extends DivideException {

    public DivideInvalidInputException(String description, Exception base) {
        super(description, base);
    }

    public DivideInvalidInputException(String description) {
        super(description);
    }

}
