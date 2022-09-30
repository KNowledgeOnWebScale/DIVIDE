package be.ugent.idlab.divide.util.query;

import be.ugent.idlab.divide.core.query.parser.DivideQueryParserInput;
import be.ugent.idlab.divide.core.query.parser.InputQueryLanguage;
import be.ugent.idlab.divide.core.query.parser.InvalidDivideQueryParserInputException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

public class DivideQueryEntryParser {

    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    public static DivideQueryEntryInQueryFormat parseSparqlEntryAsDivideQuery(String json)
            throws InvalidDivideQueryParserInputException {
        // parse DIVIDE query parser input
        DivideQueryParserInput input;
        try {
            input = GSON.fromJson(json, DivideQueryParserInput.class);
            input.setInputQueryLanguage(InputQueryLanguage.SPARQL);
        } catch (JsonSyntaxException e) {
            throw new InvalidDivideQueryParserInputException("Invalid JSON syntax", e);
        }

        // parse context enrichment entry
        JsonObject jsonObject = new JsonParser().parse(json).getAsJsonObject();
        ContextEnrichmentEntry contextEnrichmentEntry = jsonObject.get("contextEnrichment") != null ?
                GSON.fromJson(jsonObject.get("contextEnrichment").toString(),
                        ContextEnrichmentEntry.class) : null;

        return new DivideQueryEntryInQueryFormat(input, contextEnrichmentEntry);
    }

    public static DivideQueryEntryInQueryFormat parseRspQlEntryAsDivideQuery(String json)
            throws InvalidDivideQueryParserInputException {
        DivideQueryParserInput input;
        try {
            input = GSON.fromJson(json, DivideQueryParserInput.class);
            input.setInputQueryLanguage(InputQueryLanguage.RSP_QL);
        } catch (JsonSyntaxException e) {
            throw new InvalidDivideQueryParserInputException("Invalid JSON syntax", e);
        }

        // parse context enrichment entry
        JsonObject jsonObject = new JsonParser().parse(json).getAsJsonObject();
        ContextEnrichmentEntry contextEnrichmentEntry = jsonObject.get("contextEnrichment") != null ?
                GSON.fromJson(jsonObject.get("contextEnrichment").toString(),
                        ContextEnrichmentEntry.class) : null;

        return new DivideQueryEntryInQueryFormat(input, contextEnrichmentEntry);
    }

    public static DivideQueryEntryInDivideFormat parseDivideQueryEntryInDivideFormat(String json)
            throws DivideQueryEntryParserException {
        // check if any json is given:
        // if not, no context enrichment entry is defined -> return empty entry
        if (json == null || json.trim().isEmpty()) {
            return new DivideQueryEntryInDivideFormat();
        }

        // parse json
        try {
            return GSON.fromJson(json, DivideQueryEntryInDivideFormat.class);
        } catch (Exception e) {
            throw new DivideQueryEntryParserException(
                    "DIVIDE query is not in expected JSON format", e);
        }
    }

}
