package be.ugent.idlab.divide.api.endpoints.query;

import be.ugent.idlab.divide.api.endpoints.CustomEndpoint;
import be.ugent.idlab.divide.api.representation.query.DivideQueryRepresentation;
import be.ugent.idlab.divide.core.context.ContextEnrichment;
import be.ugent.idlab.divide.core.engine.IDivideEngine;
import be.ugent.idlab.divide.core.exception.DivideInvalidInputException;
import be.ugent.idlab.divide.core.exception.DivideNotInitializedException;
import be.ugent.idlab.divide.core.query.IDivideQuery;
import be.ugent.idlab.divide.core.query.parser.DivideQueryParserInput;
import be.ugent.idlab.divide.core.query.parser.DivideQueryParserOutput;
import be.ugent.idlab.divide.core.query.parser.InvalidDivideQueryParserInputException;
import be.ugent.idlab.divide.util.query.ContextEnrichmentEntry;
import be.ugent.idlab.divide.util.query.DivideQueryEntryInQueryFormat;
import be.ugent.idlab.divide.util.query.DivideQueryEntryParser;
import be.ugent.idlab.util.io.IOUtilities;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.resource.Options;
import org.restlet.resource.Post;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

public class DivideQueryRegistrationAsRspQlEndpoint extends CustomEndpoint {

    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
    private static final Logger LOGGER =
            LoggerFactory.getLogger(DivideQueryRegistrationAsRspQlEndpoint.class.getName());

    @Options
    public void optionsRequestHandler() {
        Set<Method> allowedMethods = new HashSet<>();
        allowedMethods.add(Method.POST);
        getResponse().setAccessControlAllowMethods(allowedMethods);
        getResponse().setAccessControlAllowOrigin("*");
    }

    public static void logEndpoints(Logger logger) {
        logger.info("  POST: register DIVIDE query with ID {}" +
                " (JSON description of RSP-QL input in HTTP body)", SERVER_ATTR_ID);
    }

    @Post
    public void addDivideQuery(Representation rep) {
        getResponse().setAccessControlAllowOrigin("*");

        IDivideEngine divideEngine = getDivideEngine();

        try {
            String divideQueryName = getIdAttribute();

            if (divideEngine.getDivideQueryByName(divideQueryName) == null) {

                String divideQueryJson;
                if (rep != null && (divideQueryJson = rep.getText()) != null
                        && !divideQueryJson.trim().isEmpty()) {

                    // parse JSON entry to real DIVIDE query parser & context enrichment input
                    DivideQueryEntryInQueryFormat divideQueryEntryInQueryFormat =
                            DivideQueryEntryParser.parseRspQlEntryAsDivideQuery(divideQueryJson);
                    DivideQueryParserInput divideQueryParserInput =
                            divideQueryEntryInQueryFormat.getDivideQueryParserInput();
                    ContextEnrichmentEntry contextEnrichmentEntry =
                            divideQueryEntryInQueryFormat.getContextEnrichmentEntry();

                    // parse RSP-QL input to actual DIVIDE query inputs
                    DivideQueryParserOutput divideQueryParserOutput =
                            divideEngine.getQueryParser().
                                    parseDivideQuery(divideQueryParserInput);

                    // ensure all required information is provided
                    if (divideQueryParserOutput.isNonEmpty()) {

                        // create context enrichment
                        ContextEnrichment contextEnrichment;
                        if (contextEnrichmentEntry == null
                                || contextEnrichmentEntry.getQueries() == null
                                || contextEnrichmentEntry.getQueries().isEmpty()) {
                            contextEnrichment = new ContextEnrichment();
                        } else {
                            contextEnrichment = new ContextEnrichment(
                                    contextEnrichmentEntry.doReasoning(),
                                    contextEnrichmentEntry.executeOnOntologyTriples(),
                                    contextEnrichmentEntry.getQueries());
                        }

                        // add query to DIVIDE engine
                        // (response cannot be null since it was checked before whether
                        //  query with this name already exists)
                        IDivideQuery divideQuery = divideEngine.addDivideQuery(
                                divideQueryName,
                                IOUtilities.removeWhiteSpace(
                                        divideQueryParserOutput.getQueryPattern()).replaceAll("\r", " "),
                                IOUtilities.removeWhiteSpace(
                                        divideQueryParserOutput.getSensorQueryRule()).replaceAll("\r", " "),
                                IOUtilities.removeWhiteSpace(
                                        divideQueryParserOutput.getGoal()).replaceAll("\r", " "),
                                contextEnrichment);

                        String message = "DIVIDE query with name '" + divideQueryName +
                                "' successfully registered";
                        getResponse().setStatus(Status.SUCCESS_OK, message);
                        getResponse().setEntity(
                                GSON.toJson(new DivideQueryRepresentation(divideQuery)),
                                MediaType.APPLICATION_JSON);

                    } else {
                        String message = "Input leads to empty DIVIDE query fields";
                        getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST, message);
                        getResponse().setEntity(message, MediaType.TEXT_PLAIN);
                    }

                } else {
                    String message = "No DIVIDE query JSON information specified";
                    getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST, message);
                    getResponse().setEntity(message, MediaType.TEXT_PLAIN);
                }

            } else {
                String message = "DIVIDE query with the specified name already exists";
                getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST, message);
                getResponse().setEntity(message, MediaType.TEXT_PLAIN);
            }

        } catch (InvalidDivideQueryParserInputException e) {
            String message = String.format("JSON representing RSP-QL query " +
                    "input is invalid: %s", e.getMessage());
            LOGGER.error(message, e);
            getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST, message);
            getResponse().setEntity(message, MediaType.TEXT_PLAIN);

        } catch (DivideInvalidInputException e) {
            String message = String.format("Query input invalid: %s", e.getMessage());
            LOGGER.error(message, e);
            getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST, message);
            getResponse().setEntity(message, MediaType.TEXT_PLAIN);

        } catch (DivideNotInitializedException e) {
            String message = e.getMessage();
            LOGGER.error(message, e);
            getResponse().setStatus(Status.SERVER_ERROR_SERVICE_UNAVAILABLE, message);
            getResponse().setEntity(message, MediaType.TEXT_PLAIN);

        } catch (Exception e) {
            String logMessage = "Error while adding DIVIDE query";
            String eMessage = e.getMessage();
            String message = logMessage + (eMessage != null ? ": " + eMessage : "");
            LOGGER.error(logMessage, e);
            getResponse().setStatus(Status.SERVER_ERROR_INTERNAL, message);
            getResponse().setEntity(message, MediaType.TEXT_PLAIN);

        } finally {
            getResponse().commit();
            commit();
            release();
        }
    }

}
