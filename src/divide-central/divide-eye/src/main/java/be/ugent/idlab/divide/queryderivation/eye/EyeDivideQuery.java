package be.ugent.idlab.divide.queryderivation.eye;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple class containing the EYE representation of a registered DIVIDE query.
 * It contains a reference to the N3 files containing the query pattern, the
 * sensor query rule, and the goal.
 */
class EyeDivideQuery {

    private final String queryPatternFilePath;
    private final String sensorQueryFilePath;
    private final String goalFilePath;

    private final List<String> contextEnrichingQueryFilePaths;

    EyeDivideQuery(String queryPatternFilePath,
                   String sensorQueryFilePath,
                   String goalFilePath) {
        this.queryPatternFilePath = queryPatternFilePath;
        this.sensorQueryFilePath = sensorQueryFilePath;
        this.goalFilePath = goalFilePath;

        this.contextEnrichingQueryFilePaths = new ArrayList<>();
    }

    String getQueryPatternFilePath() {
        return queryPatternFilePath;
    }

    String getSensorQueryFilePath() {
        return sensorQueryFilePath;
    }

    String getGoalFilePath() {
        return goalFilePath;
    }

    List<String> getContextEnrichingQueryFilePaths() {
        return contextEnrichingQueryFilePaths;
    }

    void addContextEnrichingQueryFilePath(String path) {
        contextEnrichingQueryFilePaths.add(path);
    }

}
