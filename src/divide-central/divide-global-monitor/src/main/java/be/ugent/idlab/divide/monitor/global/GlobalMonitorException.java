package be.ugent.idlab.divide.monitor.global;

/**
 * Exception thrown when an error occurs during any service of the DIVIDE Global Monitor.
 */
@SuppressWarnings("unused")
public class GlobalMonitorException extends Exception {

    public GlobalMonitorException(String description, Exception base) {
        super(description, base);
    }

    public GlobalMonitorException(String description) {
        super(description);
    }

}
