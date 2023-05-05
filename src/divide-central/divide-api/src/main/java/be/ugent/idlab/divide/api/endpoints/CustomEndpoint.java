package be.ugent.idlab.divide.api.endpoints;

import be.ugent.idlab.divide.api.DivideApiApplication;
import be.ugent.idlab.divide.core.engine.IDivideEngine;
import org.restlet.resource.ServerResource;

public abstract class CustomEndpoint extends ServerResource {

    public static final String SERVER_ATTR_ID = "id";
    public static final String SERVER_ATTR_NAME = "name";

    protected IDivideEngine getDivideEngine() {
        return (IDivideEngine) getContext().getAttributes().get(
                DivideApiApplication.ATTR_DIVIDE_ENGINE);
    }

    protected String getIdAttribute() {
        return (String) getRequest().getAttributes().get(SERVER_ATTR_ID);
    }

    protected String getNameAttribute() {
        return (String) getRequest().getAttributes().get(SERVER_ATTR_NAME);
    }

}
