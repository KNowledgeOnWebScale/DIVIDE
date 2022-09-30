package be.ugent.idlab.divide.api;

import be.ugent.idlab.divide.core.engine.IDivideEngine;
import org.restlet.Component;
import org.restlet.data.Protocol;

@SuppressWarnings("unused")
public class DivideApiComponentFactory {

    /**
     * Create a Restlet {@link Component} that can be started to host an API
     * for the given DIVIDE engine. This DIVIDE API will be hosted via the
     * HTTP protocol on the given host and port, on the root path,
     * i.e., at http://[host]:[port]/.
     *
     * @param divideEngine DIVIDE engine that should be wrapped by the created
     *                     API component
     * @param host host at which the DIVIDE API should run
     * @param port port at which the DIVIDE API should run
     * @return a Restlet {@link Component} which can be started with the
     *         {@link Component#start()} method to host the DIVIDE API
     */
    public static Component createRestApiComponent(IDivideEngine divideEngine,
                                                   String host,
                                                   int port) {
        return createRestApiComponent(divideEngine, host, port, "");
    }

    /**
     * Create a Restlet {@link Component} that can be started to host an API
     * for the given DIVIDE engine. This DIVIDE API will be hosted via the
     * HTTP protocol on the given host and port, on the specified uri path,
     * i.e., at http://[host]:[port]/[uri].
     *
     * @param divideEngine DIVIDE engine that should be wrapped by the created
     *                     API component
     * @param host host at which the DIVIDE API should run
     * @param port port at which the DIVIDE API should run
     * @param uri path URI string at which the DIVIDE API should run
     * @return a Restlet {@link Component} which can be started with the
     *         {@link Component#start()} method to host the DIVIDE API
     */
    public static Component createRestApiComponent(IDivideEngine divideEngine,
                                                   String host,
                                                   int port,
                                                   String uri) {
        // create Restlet component
        Component component = new Component();
        component.getServers().add(Protocol.HTTP, host, port);

        // create and attach Servlet application
        DivideApiApplication divideApiApplication = new DivideApiApplication(divideEngine);
        component.getDefaultHost().attach(uri, divideApiApplication);

        return component;
    }

}
