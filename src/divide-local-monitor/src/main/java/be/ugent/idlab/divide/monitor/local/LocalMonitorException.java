package be.ugent.idlab.divide.monitor.local;

/**
 * Exception thrown when an error occurs during any service of the DIVIDE Local Monitor.
 */
@SuppressWarnings("unused")
public class LocalMonitorException extends Exception {

    public LocalMonitorException(String description, Exception base) {
        super(description, base);
    }

    public LocalMonitorException(String description) {
        super(description);
    }

}
