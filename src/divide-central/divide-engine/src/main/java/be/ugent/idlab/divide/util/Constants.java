package be.ugent.idlab.divide.util;

import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

public class Constants {

    /**
     * Hidden directory where DIVIDE stores all its files needed to run the DIVIDE
     * engine (including the DIVIDE query derivation, monitor management, etc.)
     */
    public static final String DIVIDE_DIRECTORY = ".divide";

    // LOGGING MARKERS

    public static final Marker UNKNOWN_ERROR_MARKER = MarkerFactory.getMarker("[UNKNOWN_ERROR]");
    public static final Marker METRIC_MARKER = MarkerFactory.getMarker("[METRIC]");

}
