package be.ugent.idlab.divide.core.exception;

/**
 * Exception thrown when an error occurs during the initialization of a
 * DIVIDE object, which causes the object to not be correctly initialized,
 * and therefore prevents this object from functioning as it should.
 */
@SuppressWarnings("unused")
public class DivideInitializationException extends DivideException {

    public DivideInitializationException(String description, Exception base) {
        super(description, base);
    }

    public DivideInitializationException(String description) {
        super(description);
    }

}
