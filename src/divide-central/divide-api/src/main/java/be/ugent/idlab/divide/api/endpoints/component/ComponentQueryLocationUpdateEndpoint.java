package be.ugent.idlab.divide.api.endpoints.component;

import be.ugent.idlab.divide.api.endpoints.CustomEndpoint;
import be.ugent.idlab.divide.core.component.IComponent;
import be.ugent.idlab.divide.core.engine.IDivideEngine;
import be.ugent.idlab.divide.core.exception.DivideNotInitializedException;
import be.ugent.idlab.divide.core.query.IDivideQuery;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.resource.Options;
import org.restlet.resource.Put;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

public class ComponentQueryLocationUpdateEndpoint extends CustomEndpoint {

    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    private static final Logger LOGGER = LoggerFactory.getLogger(
            ComponentQueryLocationUpdateEndpoint.class.getName());

    @Options
    public void optionsRequestHandler() {
        Set<Method> allowedMethods = new HashSet<>();
        allowedMethods.add(Method.PUT);
        getResponse().setAccessControlAllowMethods(allowedMethods);
        getResponse().setAccessControlAllowOrigin("*");
    }

    public static void logEndpoints(Logger logger) {
        logger.info("  PUT: update location of derived queries for component" +
                        " with ID {} for DIVIDE query with name {} (location" +
                        " (one of 'central' or 'local') in plain text HTTP body)",
                SERVER_ATTR_ID, SERVER_ATTR_NAME);
    }

    @Put("text/plain")
    public void deriveQueries(Representation rep) {
        getResponse().setAccessControlAllowOrigin("*");

        IDivideEngine divideEngine = getDivideEngine();

        try {
            String componentId = getIdAttribute();
            String divideQueryName = getNameAttribute();

            IComponent component = divideEngine.getRegisteredComponentById(componentId);
            IDivideQuery divideQuery = divideEngine.getDivideQueryByName(divideQueryName);

            if (component != null) {

                if (divideQuery != null) {

                    if (rep != null) {

                        String location = rep.getText();

                        if (location != null && !location.isEmpty()) {

                            boolean moveToCentral;
                            if (location.equals("local")) {
                                moveToCentral = false;

                            } else if (location.equals("central")) {
                                moveToCentral = true;

                            } else {
                                String message = "Problem with specified location. The location must " +
                                        "be local or central.";
                                getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST, message);
                                getResponse().setEntity(GSON.toJson(message), MediaType.APPLICATION_JSON);
                                return;
                            }

                            // update location
                            divideEngine.updateQueryLocation(componentId, divideQueryName, moveToCentral);

                            String message = "Query location successfully moved to " + location;
                            getResponse().setStatus(Status.SUCCESS_OK, message);
                            getResponse().setEntity(GSON.toJson(message), MediaType.APPLICATION_JSON);

                        } else {
                            String message = "No location specified. The location must be local or central.";
                            getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST, message);
                            getResponse().setEntity(GSON.toJson(message), MediaType.APPLICATION_JSON);
                        }

                    } else {
                        String message = "No location specified. The location must be local or central.";
                        getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST, message);
                        getResponse().setEntity(GSON.toJson(message), MediaType.APPLICATION_JSON);
                    }

                } else {
                    String message = "DIVIDE query with the specified name does not exist";
                    getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST, message);
                    getResponse().setEntity(message, MediaType.TEXT_PLAIN);
                }

            } else {
                String message = "DIVIDE component with the specified ID does not exist";
                getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST, message);
                getResponse().setEntity(message, MediaType.TEXT_PLAIN);
            }

        } catch (DivideNotInitializedException e) {
            String message = e.getMessage();
            LOGGER.error(message, e);
            getResponse().setStatus(Status.SERVER_ERROR_SERVICE_UNAVAILABLE, message);
            getResponse().setEntity(message, MediaType.TEXT_PLAIN);

        } catch (Exception e) {
            String logMessage = "Error while enqueueing window parameter update";
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
