package be.ugent.idlab.divide.api;

import static be.ugent.idlab.divide.api.endpoints.CustomEndpoint.SERVER_ATTR_ID;
import static be.ugent.idlab.divide.api.endpoints.CustomEndpoint.SERVER_ATTR_NAME;

class DivideRoutes {

    private static final String COMPONENT_ENTITY = "component";
    private static final String QUERY_ENTITY = "query";

    static final String ENDPOINT_COMPONENT_GENERAL =
            "/" + COMPONENT_ENTITY;
    static final String ENDPOINT_COMPONENT =
            "/" + COMPONENT_ENTITY + "/{" + SERVER_ATTR_ID + "}";

    static final String ENDPOINT_DIVIDE_QUERY_GENERAL =
            "/" + QUERY_ENTITY;
    static final String ENDPOINT_DIVIDE_QUERY =
            "/" + QUERY_ENTITY + "/{" + SERVER_ATTR_ID + "}";
    static final String ENDPOINT_DIVIDE_QUERY_REGISTER_AS_SPARQL =
            "/" + QUERY_ENTITY + "/sparql/{" + SERVER_ATTR_ID + "}";
    static final String ENDPOINT_DIVIDE_QUERY_REGISTER_AS_RSP_QL =
            "/" + QUERY_ENTITY + "/rspql/{" + SERVER_ATTR_ID + "}";

    static final String ENDPOINT_DIVIDE_QUERY_DERIVATION =
            "/" + COMPONENT_ENTITY + "/{" + SERVER_ATTR_ID + "}/derive/"
                    + QUERY_ENTITY + "/{" + SERVER_ATTR_NAME + "}";
    static final String ENDPOINT_DIVIDE_QUERY_LOCATION_UPDATE =
            "/" + COMPONENT_ENTITY + "/{" + SERVER_ATTR_ID + "}/update_location/"
                    + QUERY_ENTITY + "/{" + SERVER_ATTR_NAME + "}";

}
