package be.ugent.idlab.divide.core.exception;

/**
 * General exception describing known DIVIDE errors.
 */
public abstract class DivideException extends Exception {

    public DivideException(String description, Exception base) {
        super(description, base);
    }

    public DivideException(String description) {
        super(description);
    }

    public DivideException(Exception base) {
        super(base);
    }

}
