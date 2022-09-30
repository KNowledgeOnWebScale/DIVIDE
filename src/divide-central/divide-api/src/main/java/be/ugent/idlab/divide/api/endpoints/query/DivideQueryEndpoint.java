package be.ugent.idlab.divide.api.endpoints.query;

import be.ugent.idlab.divide.api.endpoints.CustomEndpoint;
import be.ugent.idlab.divide.api.representation.query.DivideQueryRepresentation;
import be.ugent.idlab.divide.core.context.ContextEnrichment;
import be.ugent.idlab.divide.core.engine.IDivideEngine;
import be.ugent.idlab.divide.core.exception.DivideInvalidInputException;
import be.ugent.idlab.divide.core.exception.DivideNotInitializedException;
import be.ugent.idlab.divide.core.query.IDivideQuery;
import be.ugent.idlab.divide.util.query.ContextEnrichmentEntry;
import be.ugent.idlab.divide.util.query.DivideQueryEntryInDivideFormat;
import be.ugent.idlab.divide.util.query.DivideQueryEntryParser;
import be.ugent.idlab.util.io.IOUtilities;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.Options;
import org.restlet.resource.Post;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

public class DivideQueryEndpoint extends CustomEndpoint {

    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
    private static final Logger LOGGER = LoggerFactory.getLogger(DivideQueryEndpoint.class.getName());

    @Options
    public void optionsRequestHandler() {
        Set<Method> allowedMethods = new HashSet<>();
        allowedMethods.add(Method.GET);
        allowedMethods.add(Method.DELETE);
        allowedMethods.add(Method.POST);
        getResponse().setAccessControlAllowMethods(allowedMethods);
        getResponse().setAccessControlAllowOrigin("*");
    }

    public static void logEndpoints(Logger logger) {
        logger.info("  GET: retrieve DIVIDE query with ID {}", SERVER_ATTR_ID);
        logger.info("  POST: register DIVIDE query with ID {}" +
                " (JSON description of DIVIDE query inputs in HTTP body)", SERVER_ATTR_ID);
        logger.info("  DELETE: unregister DIVIDE query with ID {}", SERVER_ATTR_ID);
    }

    @Get
    public void getDivideQuery() {
        getResponse().setAccessControlAllowOrigin("*");

        IDivideEngine divideEngine = getDivideEngine();

        try {
            String divideQueryName = getIdAttribute();

            IDivideQuery divideQuery = divideEngine.getDivideQueryByName(divideQueryName);

            if (divideQuery != null) {
                DivideQueryRepresentation divideQueryRepresentation =
                        new DivideQueryRepresentation(divideQuery);

                String message = "DIVIDE query with name '" + divideQueryName + "' successfully retrieved";
                getResponse().setStatus(Status.SUCCESS_OK, message);
                getResponse().setEntity(GSON.toJson(divideQueryRepresentation),
                        MediaType.APPLICATION_JSON);

            } else {
                String message = "DIVIDE query with name '" + divideQueryName + "' does not exist";
                getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND, message);
                getResponse().setEntity(message, MediaType.TEXT_PLAIN);
            }

        } catch (DivideNotInitializedException e) {
            String message = e.getMessage();
            LOGGER.error(message, e);
            getResponse().setStatus(Status.SERVER_ERROR_SERVICE_UNAVAILABLE, message);
            getResponse().setEntity(message, MediaType.TEXT_PLAIN);

        } catch (Exception e) {
            String logMessage = "Error while getting DIVIDE query data";
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

    @Post
    public void addDivideQuery(Representation rep) {
        getResponse().setAccessControlAllowOrigin("*");

        IDivideEngine divideEngine = getDivideEngine();

        try {
            String divideQueryName = getIdAttribute();

            if (divideEngine.getDivideQueryByName(divideQueryName) == null) {

                if (rep != null) {
                    String divideQueryJson = rep.getText();

                    DivideQueryEntryInDivideFormat divideQueryEntry;
                    String queryPattern;
                    String sensorQueryRule;
                    String goal;
                    try {
                        // parse DIVIDE query JSON
                        divideQueryEntry = DivideQueryEntryParser.parseDivideQueryEntryInDivideFormat(divideQueryJson);
                        queryPattern = divideQueryEntry.getQueryPattern();
                        sensorQueryRule = divideQueryEntry.getSensorQueryRule();
                        goal = divideQueryEntry.getGoal();

                    } catch (Exception e) {
                        String message = "Specified DIVIDE query information is no valid JSON";
                        LOGGER.error(message, e);
                        getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST, message);
                        getResponse().setEntity(message, MediaType.TEXT_PLAIN);
                        return;
                    }

                    // ensure all required information is provided
                    if (queryPattern != null && !queryPattern.isEmpty() &&
                            sensorQueryRule != null && !sensorQueryRule.isEmpty() &&
                            goal != null && !goal.isEmpty()) {

                        // create context enrichment
                        ContextEnrichmentEntry contextEnrichmentEntry =
                                divideQueryEntry.getContextEnrichment();
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
                                IOUtilities.removeWhiteSpace(queryPattern).replaceAll("\r", " "),
                                IOUtilities.removeWhiteSpace(sensorQueryRule).replaceAll("\r", " "),
                                IOUtilities.removeWhiteSpace(goal).replaceAll("\r", " "),
                                contextEnrichment);

                        String message = "DIVIDE query with name '" + divideQueryName +
                                "' successfully registered";
                        getResponse().setStatus(Status.SUCCESS_OK, message);
                        getResponse().setEntity(
                                GSON.toJson(new DivideQueryRepresentation(divideQuery)),
                                MediaType.APPLICATION_JSON);

                    } else {
                        String message = "Not all required DIVIDE query JSON information " +
                                "is specified and non-empty";
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

    @Delete
    public void removeDivideQuery(Representation rep) {
        getResponse().setAccessControlAllowOrigin("*");

        IDivideEngine divideEngine = getDivideEngine();

        try {
            String divideQueryName = getIdAttribute();

            IDivideQuery divideQuery = divideEngine.getDivideQueryByName(divideQueryName);

            if (divideQuery != null) {
                // retrieve url parameter which specifies whether the queries of this
                // DIVIDE query should be unregistered
                // (default when it is not specified = true)
                boolean unregisterQueries = !"false".equals(getQueryValue("unregister"));

                divideEngine.removeDivideQuery(divideQueryName, unregisterQueries);

                String message = "DIVIDE query with name " + divideQueryName + " successfully unregistered";
                getResponse().setStatus(Status.SUCCESS_NO_CONTENT, message);

            } else {
                String message = "DIVIDE query with name '" + divideQueryName + "' does not exist";
                getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND, message);
                getResponse().setEntity(message, MediaType.TEXT_PLAIN);
            }

        } catch (DivideNotInitializedException e) {
            String message = e.getMessage();
            LOGGER.error(message, e);
            getResponse().setStatus(Status.SERVER_ERROR_SERVICE_UNAVAILABLE, message);
            getResponse().setEntity(message, MediaType.TEXT_PLAIN);

        } catch (Exception e) {
            String logMessage = "Error while removing DIVIDE query";
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
