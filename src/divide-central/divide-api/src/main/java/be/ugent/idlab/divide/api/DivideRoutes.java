package be.ugent.idlab.divide.api;

import static be.ugent.idlab.divide.api.endpoints.CustomEndpoint.SERVER_ATTR_ID;

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

}
