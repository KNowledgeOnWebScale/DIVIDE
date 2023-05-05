package be.ugent.idlab.divide.rsp.query.window;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StreamWindow implements IStreamWindow {

    private static final Logger LOGGER = LoggerFactory.getLogger(StreamWindow.class.getName());

    private static final Pattern RSP_QL_WINDOW_PARAMETERS_PATTERN = Pattern.compile(
            "\\s*((RANGE\\s+(\\S+))|(FROM\\s+NOW-(\\S+)\\s+TO\\s+NOW-(\\S+)))\\s+(TUMBLING|((STEP|SLIDE)\\s+(\\S+)))",
            Pattern.CASE_INSENSITIVE);

    private final String streamIri;
    private final String windowDefinition;
    private final int windowSizeInSeconds;
    private final int windowStartInSecondsAgo;
    private final int windowEndInSecondsAgo;
    private final int querySlidingStepInSeconds;

    public StreamWindow(String streamIri,
                        String windowDefinition) {
        try {
            this.streamIri = streamIri;
            this.windowDefinition = windowDefinition;

            // parse window definition
            Matcher m = RSP_QL_WINDOW_PARAMETERS_PATTERN.matcher(windowDefinition);
            if (m.find()) {
                // extract all regex groups
                String range = m.group(3);
                String from = m.group(5);
                String to = m.group(6);
                String stepString = m.group(7);
                String step = m.group(10);

                // check if range is specified with a range string or a from-to
                if (range != null) {
                    this.windowSizeInSeconds = parseDurationStringToSeconds(range);
                    this.windowStartInSecondsAgo = -1;
                    this.windowEndInSecondsAgo = -1;
                } else {
                    this.windowSizeInSeconds = -1;
                    this.windowStartInSecondsAgo = parseDurationStringToSeconds(from);
                    this.windowEndInSecondsAgo = parseDurationStringToSeconds(to);
                }

                // check if window slide is tumbling
                if ("TUMBLING".equals(stepString)) {
                    // if this is the case, the window size should be defined
                    if (this.windowSizeInSeconds == -1) {
                        throw new StreamWindowParsingException("tumbling window not allowed with FROM-TO");
                    }
                    this.querySlidingStepInSeconds = this.windowSizeInSeconds;
                } else {
                    this.querySlidingStepInSeconds = parseDurationStringToSeconds(step);
                }

            } else {
                throw new StreamWindowParsingException("window definition has no valid syntax");
            }

        } catch (Exception e) {
            String message = String.format("Window definition %s on stream %s is no valid RSP-QL: %s",
                    windowDefinition, streamIri, e.getMessage());
            LOGGER.error(message, e);
            throw new RuntimeException(message);
        }
    }

    private int parseDurationStringToSeconds(String durationString) throws StreamWindowParsingException {
        // preprocess string
        if (durationString.startsWith("PT")) {
            durationString = durationString.substring(2);
        }
        durationString = durationString.toUpperCase();

        // extract number & unit
        int number = Integer.parseInt(durationString.substring(0, durationString.length() - 1));
        String unit = durationString.substring(durationString.length() - 1);

        // convert number to seconds based on unit
        switch (unit) {
            case "S":
                return number;
            case "M":
                return number * 60;
            case "H":
                return number * 60 * 60;
            default:
                throw new StreamWindowParsingException("invalid unit used");
        }
    }

    @Override
    public String getStreamIri() {
        return streamIri;
    }

    @Override
    public String getWindowDefinition() {
        return windowDefinition;
    }

    @Override
    public int getWindowSizeInSeconds() {
        return windowSizeInSeconds;
    }

    @Override
    public int getWindowStartInSecondsAgo() {
        return windowStartInSecondsAgo;
    }

    @Override
    public int getWindowEndInSecondsAgo() {
        return windowEndInSecondsAgo;
    }

    @Override
    public int getQuerySlidingStepInSeconds() {
        return querySlidingStepInSeconds;
    }

    @Override
    public String toString() {
        return "StreamWindow{" +
                "streamIri='" + streamIri + '\'' +
                ", windowDefinition='" + windowDefinition + '\'' +
                ", windowSizeInSeconds=" + windowSizeInSeconds +
                ", windowStartInSecondsAgo=" + windowStartInSecondsAgo +
                ", windowEndInSecondsAgo=" + windowEndInSecondsAgo +
                ", querySlidingStepInSeconds=" + querySlidingStepInSeconds +
                '}';
    }

}
