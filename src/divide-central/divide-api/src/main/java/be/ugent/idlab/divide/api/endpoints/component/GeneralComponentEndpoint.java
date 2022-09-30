package be.ugent.idlab.divide.api.endpoints.component;

import be.ugent.idlab.divide.api.endpoints.CustomEndpoint;
import be.ugent.idlab.divide.api.representation.component.ComponentRepresentation;
import be.ugent.idlab.divide.core.component.IComponent;
import be.ugent.idlab.divide.core.engine.IDivideEngine;
import be.ugent.idlab.divide.core.exception.DivideInvalidInputException;
import be.ugent.idlab.divide.core.exception.DivideNotInitializedException;
import be.ugent.idlab.divide.util.component.ComponentEntry;
import be.ugent.idlab.divide.util.component.ComponentEntryParserException;
import be.ugent.idlab.divide.util.component.JsonComponentEntryParser;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.Options;
import org.restlet.resource.Post;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class GeneralComponentEndpoint extends CustomEndpoint {

    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
    private static final Logger LOGGER = LoggerFactory.getLogger(GeneralComponentEndpoint.class.getName());

    @Options
    public void optionsRequestHandler() {
        Set<Method> allowedMethods = new HashSet<>();
        allowedMethods.add(Method.GET);
        allowedMethods.add(Method.POST);
        getResponse().setAccessControlAllowMethods(allowedMethods);
        getResponse().setAccessControlAllowOrigin("*");
    }

    public static void logEndpoints(Logger logger) {
        logger.info("  GET: retrieve all registered DIVIDE components");
        logger.info("  POST: register a new DIVIDE component (description in HTTP body)");
    }

    @Get
    public void getComponents() {
        getResponse().setAccessControlAllowOrigin("*");

        IDivideEngine divideEngine = getDivideEngine();

        try {
            Collection<ComponentRepresentation> components = new ArrayList<>();

            for (IComponent component : divideEngine.getRegisteredComponents()) {
                components.add(new ComponentRepresentation(component));
            }

            String message = "Components successfully retrieved";
            getResponse().setStatus(Status.SUCCESS_OK, message);
            getResponse().setEntity(GSON.toJson(components), MediaType.APPLICATION_JSON);

        } catch (DivideNotInitializedException e) {
            String message = e.getMessage();
            LOGGER.error(message, e);
            getResponse().setStatus(Status.SERVER_ERROR_SERVICE_UNAVAILABLE, message);
            getResponse().setEntity(message, MediaType.TEXT_PLAIN);

        } catch (Exception e) {
            String logMessage = "Error while getting component data";
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
    public void registerComponent(Representation rep) {
        getResponse().setAccessControlAllowOrigin("*");

        IDivideEngine divideEngine = getDivideEngine();

        try {
            if (rep != null) {
                String componentEntryString = rep.getText();

                // parse component entry
                ComponentEntry componentEntry =
                        JsonComponentEntryParser.parseComponentEntry(componentEntryString);

                // register component
                IComponent component = divideEngine.registerComponent(
                        new ArrayList<>(componentEntry.getContextIris()),
                        componentEntry.getRspQueryLanguage(),
                        componentEntry.getRspEngineUrl());

                if (component != null) {
                    String message = "Component with ID " + component.getId() +
                            " successfully registered";
                    getResponse().setStatus(Status.SUCCESS_OK, message);
                    getResponse().setEntity(
                            GSON.toJson(new ComponentRepresentation(component)),
                            MediaType.APPLICATION_JSON);

                } else {
                    String message = "Component with the specified host, port and path of the " +
                            "RSP engine URL already exists";
                    getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST, message);
                    getResponse().setEntity(message, MediaType.TEXT_PLAIN);
                }

            } else {
                String message = "No component entry information specified";
                getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST, message);
                getResponse().setEntity(message, MediaType.TEXT_PLAIN);
            }

        } catch (ComponentEntryParserException e) {
            String message = String.format("Component entry information invalid: %s", e.getMessage());
            LOGGER.error(message, e);
            getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST, message);
            getResponse().setEntity(message, MediaType.TEXT_PLAIN);

        } catch (DivideInvalidInputException e) {
            String message = String.format("Component input invalid: %s", e.getMessage());
            LOGGER.error(message, e);
            getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST, message);
            getResponse().setEntity(message, MediaType.TEXT_PLAIN);

        } catch (DivideNotInitializedException e) {
            String message = e.getMessage();
            LOGGER.error(message, e);
            getResponse().setStatus(Status.SERVER_ERROR_SERVICE_UNAVAILABLE, message);
            getResponse().setEntity(message, MediaType.TEXT_PLAIN);

        } catch (Exception e) {
            String logMessage = "Error while registering component";
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
