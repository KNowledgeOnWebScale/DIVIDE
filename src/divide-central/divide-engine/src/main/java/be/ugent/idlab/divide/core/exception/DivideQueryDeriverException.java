package be.ugent.idlab.divide.core.exception;

/**
 * Exception thrown when an error occurs related to the query derivation process
 * of DIVIDE. This can be during the query derivation, but also during the
 * registration of DIVIDE queries in preparation of the query derivation for
 * these DIVIDE queries.
 */
@SuppressWarnings("unused")
public class DivideQueryDeriverException extends DivideException {

    public DivideQueryDeriverException(String description, Exception base) {
        super(description, base);
    }

    public DivideQueryDeriverException(String description) {
        super(description);
    }

    public DivideQueryDeriverException(Exception base) {
        super(base);
    }

}
