package be.ugent.idlab.divide.monitor;

/**
 * Exception thrown when an error occurs during the management of the DIVIDE Monitor.
 */
@SuppressWarnings("unused")
public class MonitorException extends Exception {

    public MonitorException(String description, Exception base) {
        super(description, base);
    }

    public MonitorException(String description) {
        super(description);
    }

}
