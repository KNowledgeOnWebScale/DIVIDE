package be.ugent.idlab.divide.core.component;

import be.ugent.idlab.divide.core.engine.IDivideEngine;
import be.ugent.idlab.divide.core.exception.DivideInvalidInputException;
import be.ugent.idlab.divide.rsp.IRspEngineHandler;
import be.ugent.idlab.divide.rsp.RspEngineHandlerFactory;
import be.ugent.idlab.divide.rsp.RspQueryLanguage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@SuppressWarnings({"UnusedReturnValue", "unused"})
public class ComponentFactory {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(ComponentFactory.class.getName());

    /**
     * Creates an {@link IComponent} instance with the given inputs.
     *
     * @param contextIris IRIs of the ABoxes in a knowledge base that represent the relevant
     *                    context associated to the new {@link IComponent}
     * @param localRspQueryLanguage RSP query language used by the RSP engine running on
     *                              the created component
     * @return the new {@link IComponent}
     * @throws DivideInvalidInputException if the RSP engine URL is no valid URL
     */
    public static IComponent createInstance(String ipAddress,
                                            List<String> contextIris,
                                            RspQueryLanguage localRspQueryLanguage,
                                            int localRspEngineServerPort,
                                            IDivideEngine divideEngine)
            throws DivideInvalidInputException {
        // construct the URL to communicate with the local RSP engine
        String localRspEngineUrl = String.format("http://%s:%d", ipAddress, localRspEngineServerPort);

        // create a unique component ID which is a modified version of the RSP engine
        // URL that is file system friendly (i.e., that can be
        // used in file names and directory names)
        String componentId;
        try {
            URL url = new URL(localRspEngineUrl);
            componentId = String.format("%s-%d-%s",
                    url.getHost(),
                    url.getPort() != -1 ? url.getPort() : 80,
                    URLEncoder.encode(url.getPath(), StandardCharsets.UTF_8.toString()).
                            replaceAll("%", ""));
        } catch (MalformedURLException | UnsupportedEncodingException e) {
            // should never occur since the URL has been validated when creating the
            // IRspEngineHandler above
            LOGGER.error("The created component is null, so an unknown input validation " +
                    "error has occurred");
            throw new DivideInvalidInputException("An unknown input validation error has occurred", e);
        }

        // create a handler for the RSP engine running on the new component
        IRspEngineHandler rspEngineHandler = RspEngineHandlerFactory.createInstance(
                localRspQueryLanguage, localRspEngineUrl, localRspEngineServerPort, componentId, divideEngine);

        return new Component(componentId, ipAddress, rspEngineHandler, contextIris);
    }

}
