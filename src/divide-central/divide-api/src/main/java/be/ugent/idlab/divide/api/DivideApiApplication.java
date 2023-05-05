package be.ugent.idlab.divide.api;

import be.ugent.idlab.divide.api.endpoints.component.ComponentEndpoint;
import be.ugent.idlab.divide.api.endpoints.component.ComponentQueryDerivationEndpoint;
import be.ugent.idlab.divide.api.endpoints.component.ComponentQueryLocationUpdateEndpoint;
import be.ugent.idlab.divide.api.endpoints.component.GeneralComponentEndpoint;
import be.ugent.idlab.divide.api.endpoints.query.DivideQueryEndpoint;
import be.ugent.idlab.divide.api.endpoints.query.DivideQueryRegistrationAsRspQlEndpoint;
import be.ugent.idlab.divide.api.endpoints.query.DivideQueryRegistrationAsSparqlEndpoint;
import be.ugent.idlab.divide.api.endpoints.query.GeneralDivideQueryEndpoint;
import be.ugent.idlab.divide.core.engine.IDivideEngine;
import org.restlet.Application;
import org.restlet.Restlet;
import org.restlet.routing.Router;
import org.restlet.routing.Template;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DivideApiApplication extends Application {

    private static final Logger LOGGER = LoggerFactory.getLogger(DivideApiApplication.class.getName());

    public static final String ATTR_DIVIDE_ENGINE = "divide_engine";

    private final IDivideEngine divideEngine;

    public DivideApiApplication(IDivideEngine divideEngine) {
        this.divideEngine = divideEngine;
    }

    @Override
    public Restlet createInboundRoot() {
        getContext().getAttributes().put(ATTR_DIVIDE_ENGINE, divideEngine);

        Router router = new Router(getContext());
        router.setDefaultMatchingMode(Template.MODE_EQUALS);

        router.attach(DivideRoutes.ENDPOINT_COMPONENT, ComponentEndpoint.class);
        LOGGER.info("DIVIDE API endpoint {}", DivideRoutes.ENDPOINT_COMPONENT);
        ComponentEndpoint.logEndpoints(LOGGER);

        router.attach(DivideRoutes.ENDPOINT_COMPONENT_GENERAL, GeneralComponentEndpoint.class);
        LOGGER.info("DIVIDE API endpoint {}", DivideRoutes.ENDPOINT_COMPONENT_GENERAL);
        GeneralComponentEndpoint.logEndpoints(LOGGER);

        router.attach(DivideRoutes.ENDPOINT_DIVIDE_QUERY, DivideQueryEndpoint.class);
        LOGGER.info("DIVIDE API endpoint {}", DivideRoutes.ENDPOINT_DIVIDE_QUERY);
        DivideQueryEndpoint.logEndpoints(LOGGER);

        router.attach(DivideRoutes.ENDPOINT_DIVIDE_QUERY_REGISTER_AS_SPARQL,
                DivideQueryRegistrationAsSparqlEndpoint.class);
        LOGGER.info("DIVIDE API endpoint {}", DivideRoutes.ENDPOINT_DIVIDE_QUERY_REGISTER_AS_SPARQL);
        DivideQueryRegistrationAsSparqlEndpoint.logEndpoints(LOGGER);

        router.attach(DivideRoutes.ENDPOINT_DIVIDE_QUERY_REGISTER_AS_RSP_QL,
                DivideQueryRegistrationAsRspQlEndpoint.class);
        LOGGER.info("DIVIDE API endpoint {}", DivideRoutes.ENDPOINT_DIVIDE_QUERY_REGISTER_AS_RSP_QL);
        DivideQueryRegistrationAsRspQlEndpoint.logEndpoints(LOGGER);

        router.attach(DivideRoutes.ENDPOINT_DIVIDE_QUERY_GENERAL, GeneralDivideQueryEndpoint.class);
        LOGGER.info("DIVIDE API endpoint {}", DivideRoutes.ENDPOINT_DIVIDE_QUERY_GENERAL);
        GeneralDivideQueryEndpoint.logEndpoints(LOGGER);

        router.attach(DivideRoutes.ENDPOINT_DIVIDE_QUERY_DERIVATION, ComponentQueryDerivationEndpoint.class);
        LOGGER.info("DIVIDE API endpoint {}", DivideRoutes.ENDPOINT_DIVIDE_QUERY_DERIVATION);
        ComponentQueryDerivationEndpoint.logEndpoints(LOGGER);

        router.attach(DivideRoutes.ENDPOINT_DIVIDE_QUERY_LOCATION_UPDATE, ComponentQueryLocationUpdateEndpoint.class);
        LOGGER.info("DIVIDE API endpoint {}", DivideRoutes.ENDPOINT_DIVIDE_QUERY_LOCATION_UPDATE);
        ComponentQueryLocationUpdateEndpoint.logEndpoints(LOGGER);

        return router;
    }

}