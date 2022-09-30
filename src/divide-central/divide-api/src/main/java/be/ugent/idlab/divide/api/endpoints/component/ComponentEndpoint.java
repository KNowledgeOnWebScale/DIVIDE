package be.ugent.idlab.divide.api.endpoints.component;

import be.ugent.idlab.divide.api.endpoints.CustomEndpoint;
import be.ugent.idlab.divide.api.representation.component.ComponentRepresentation;
import be.ugent.idlab.divide.core.component.IComponent;
import be.ugent.idlab.divide.core.engine.IDivideEngine;
import be.ugent.idlab.divide.core.exception.DivideNotInitializedException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

public class ComponentEndpoint extends CustomEndpoint {

    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
    private static final Logger LOGGER = LoggerFactory.getLogger(ComponentEndpoint.class.getName());

    @Options
    public void optionsRequestHandler() {
        Set<Method> allowedMethods = new HashSet<>();
        allowedMethods.add(Method.GET);
        allowedMethods.add(Method.DELETE);
        getResponse().setAccessControlAllowMethods(allowedMethods);
        getResponse().setAccessControlAllowOrigin("*");
    }

    public static void logEndpoints(Logger logger) {
        logger.info("  GET: retrieve DIVIDE component with ID {}", SERVER_ATTR_ID);
        logger.info("  DELETE: unregister DIVIDE component with ID {}", SERVER_ATTR_ID);
    }

    @Get
    public void getComponent() {
        getResponse().setAccessControlAllowOrigin("*");

        IDivideEngine divideEngine = getDivideEngine();

        try {
            String componentId = getIdAttribute();

            IComponent component = divideEngine.getRegisteredComponentById(componentId);

            if (component != null) {
                ComponentRepresentation componentRepresentation =
                        new ComponentRepresentation(component);

                String message = "Component with ID " + componentId + " successfully retrieved";
                getResponse().setStatus(Status.SUCCESS_OK, message);
                getResponse().setEntity(GSON.toJson(componentRepresentation),
                        MediaType.APPLICATION_JSON);

            } else {
                String message = "Component with ID '" + componentId + "' does not exist";
                getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND, message);
                getResponse().setEntity(message, MediaType.TEXT_PLAIN);
            }

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

    @Delete
    public void unregisterComponent(Representation rep) {
        getResponse().setAccessControlAllowOrigin("*");

        IDivideEngine divideEngine = getDivideEngine();

        try {
            String componentId = getIdAttribute();

            IComponent component = divideEngine.getRegisteredComponentById(componentId);

            if (component != null) {
                // retrieve url parameter which specifies whether the queries of this
                // component should be unregistered
                // (default when it is not specified = false)
                boolean unregisterQueries =
                        Boolean.parseBoolean(getQueryValue("unregister"));

                divideEngine.unregisterComponent(componentId, unregisterQueries);

                String message = "Component with ID " + componentId + " successfully unregistered";
                getResponse().setStatus(Status.SUCCESS_NO_CONTENT, message);

            } else {
                String message = "Component with ID '" + componentId + "' does not exist";
                getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND, message);
                getResponse().setEntity(message, MediaType.TEXT_PLAIN);
            }

        } catch (DivideNotInitializedException e) {
            String message = e.getMessage();
            LOGGER.error(message, e);
            getResponse().setStatus(Status.SERVER_ERROR_SERVICE_UNAVAILABLE, message);
            getResponse().setEntity(message, MediaType.TEXT_PLAIN);

        } catch (Exception e) {
            String logMessage = "Error while unregistering component";
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
