package be.ugent.idlab.divide.util.component;

import be.ugent.idlab.divide.core.engine.IDivideEngine;
import be.ugent.idlab.divide.rsp.RspQueryLanguage;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Class used for parsing configuration of component entries.
 * These configurations can be present in a CSV file (e.g. used during start-up
 * of a DIVIDE server with a known set of components) or in the HTTP body of
 * a component creation request to the DIVIDE API.
 */
public class JsonComponentEntryParser {

    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    /**
     * Parses a DIVIDE component entry, which is specified in JSON format.
     *
     * @param json component configuration as a JSON string
     * @return a parsed component entry of which the gettable fields can
     *         directly be used as input for the registration of components in a
     *         DIVIDE engine using the {@link IDivideEngine#registerComponent(
     *         List, RspQueryLanguage, String)} method
     * @throws ComponentEntryParserException if the specified component configuration is
     *                                       not in the required JSON format
     */
    public static ComponentEntry parseComponentEntry(String json)
            throws ComponentEntryParserException {
        // parse json
        JsonComponentEntry jsonComponentEntry =
                GSON.fromJson(json, JsonComponentEntry.class);

        // check if all fields are non-null
        boolean valid = jsonComponentEntry.validateIfNonNull();
        if (!valid) {
            throw new ComponentEntryParserException("Not all required JSON fields are present");
        }

        return parseComponentEntry(jsonComponentEntry);
    }

    private static ComponentEntry parseComponentEntry(JsonComponentEntry jsonComponentEntry)
            throws ComponentEntryParserException {
        // parse context IRIs
        List<String> contextIris = jsonComponentEntry.getContextIris().stream()
                .map(String::trim)
                .collect(Collectors.toList());
        ComponentEntryParser.validateContextIris(contextIris);

        // parse RSP query language
        RspQueryLanguage rspQueryLanguage =
                ComponentEntryParser.parseRspEngineQueryLanguage(
                        jsonComponentEntry.getRspEngine().getQueryLanguage());

        // parse RSP engine registration URL
        String rspEngineUrl = jsonComponentEntry.getRspEngine().getUrl();
        ComponentEntryParser.validateRspEngineUrl(rspEngineUrl);

        // if no errors, then return new component entry
        return new ComponentEntry(
                contextIris,
                rspQueryLanguage,
                rspEngineUrl);
    }

}
