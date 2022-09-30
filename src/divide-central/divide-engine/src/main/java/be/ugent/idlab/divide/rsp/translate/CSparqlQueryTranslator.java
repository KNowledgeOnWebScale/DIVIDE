package be.ugent.idlab.divide.rsp.translate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class capable of translating an RSP-QL query to C-SPARQL format.
 */
public class CSparqlQueryTranslator implements IQueryTranslator {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(CSparqlQueryTranslator.class.getName());

    private static final Pattern STREAM_PATTERN = Pattern.compile(
            "FROM\\s+NAMED\\s+WINDOW\\s+\\S+\\s+ON\\s+(\\S+)\\s+(\\[[^\\[\\]]+])",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern WHERE_CLAUSE_PATTERN = Pattern.compile(
            "WHERE\\s+\\{[\\s\\S]*}",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern WINDOW_START_PATTERN = Pattern.compile(
            "WINDOW\\s+\\S+\\s+\\{",
            Pattern.CASE_INSENSITIVE);

    @Override
    public String translateQuery(String queryBody, String queryName) {
        LOGGER.info("Translating query '{}' to C-SPARQL syntax", queryName);

        String result = queryBody;

        // translate stream declarations
        Matcher m1 = STREAM_PATTERN.matcher(queryBody);
        while (m1.find()) {
            String streamPart = m1.group(0);
            String streamName = m1.group(1);
            String streamParameters = m1.group(2).toLowerCase()
                    .replace("pt", "")
                    .replace("range", "RANGE")
                    .replace("tumbling", "TUMBLING")
                    .replace("from", "FROM")
                    .replace("now", "NOW")
                    .replace("to", "TO")
                    .replace("slide", "STEP")
                    .replace("step", "STEP");
            result = result.replace(streamPart,
                    String.format("FROM STREAM %s %s", streamName, streamParameters));
        }

        // aggregate all windows in WHERE clause
        Matcher m2 = WHERE_CLAUSE_PATTERN.matcher(queryBody);
        if (m2.find()) {
            String whereClause = m2.group(0);

            Matcher m3 = WINDOW_START_PATTERN.matcher(queryBody);

            List<Integer> indicesToRemove = new ArrayList<>();

            while (m3.find()) {

                String windowStart = m3.group(0);
                int windowStartIndex = whereClause.indexOf(windowStart) + windowStart.length();

                for (int i = whereClause.indexOf(windowStart); i < windowStartIndex; i++) {
                    indicesToRemove.add(i);
                }

                int braceLevels = 1;
                for (int i = windowStartIndex; i < whereClause.length(); i++) {
                    char c = whereClause.charAt(i);
                    if (c == '{') {
                        braceLevels++;
                    } else if (c == '}') {
                        braceLevels--;
                        if (braceLevels == 0) {

                            indicesToRemove.add(i);

                            break;
                        }
                    }
                }
            }

            StringBuilder newWhereClause = new StringBuilder();

            indicesToRemove.sort(Integer::compareTo);

            int previousIndex = 0;
            for (Integer indexToRemove : indicesToRemove) {
                newWhereClause.append(whereClause, previousIndex, indexToRemove);
                previousIndex = indexToRemove + 1;
            }
            newWhereClause.append(whereClause, previousIndex, whereClause.length());

            result = result.replace(whereClause, newWhereClause.toString());
        }

        // add registration clause
        result = "REGISTER QUERY " + queryName + " AS \n" + result;

        return result;
    }

}
