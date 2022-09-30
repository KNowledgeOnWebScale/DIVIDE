package be.ugent.idlab.divide.core.exception;

/**
 * Exception thrown when a method of a DIVIDE object is called when it has not
 * been initialized, and this object should be initialized first before this
 * method can be called.
 */
@SuppressWarnings("unused")
public class DivideNotInitializedException extends DivideException {

    public DivideNotInitializedException(String description, Exception base) {
        super(description, base);
    }

    public DivideNotInitializedException(String description) {
        super(description);
    }

    public DivideNotInitializedException(Exception base) {
        super("DIVIDE engine has not been initialized", base);
    }

    public DivideNotInitializedException() {
        super("DIVIDE engine has not been initialized");
    }

}
