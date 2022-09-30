package be.ugent.idlab.divide.core.component;

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
     * @param rspQueryLanguage RSP query language used by the RSP engine running on
     *                         the created component
     * @param rspEngineUrl URL which should be used for communication with the RSP engine
     *                     running on the created component, and which will also be mapped
     *                     to a unique ID for the created component
     * @return the new {@link IComponent}
     * @throws DivideInvalidInputException if the RSP engine URL is no valid URL
     */
    public static IComponent createInstance(List<String> contextIris,
                                            RspQueryLanguage rspQueryLanguage,
                                            String rspEngineUrl)
            throws DivideInvalidInputException {
        // create a handler for the RSP engine running on the new component
        // (this includes a validation of the URL to communicate with the engine later on)
        IRspEngineHandler rspEngine = RspEngineHandlerFactory.createInstance(
                rspQueryLanguage, rspEngineUrl);

        // update RSP engine URL to validated & preprocessed URL
        rspEngineUrl = rspEngine.getRspEngine().getBaseUrl();

        // create a unique ID which is a modified version of the RSP engine
        // URL that is file system friendly (i.e., that can be
        // used in file names and directory names)
        String id;
        try {
            URL url = new URL(rspEngineUrl);
            id = String.format("%s-%d-%s",
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

        return new Component(id, rspEngine, contextIris);
    }

}
