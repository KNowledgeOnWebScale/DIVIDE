package be.ugent.idlab.divide.rsp;

import be.ugent.idlab.divide.core.exception.DivideInvalidInputException;

public class RspEngineHandlerFactory {

    /**
     * Creates an {@link IRspEngineHandler} object for an RSP engine
     * with the given query language and registration URL.
     * @param rspQueryLanguage query language used by the RSP engine
     * @param url base URL which will be used for communication with the RSP engine
     * @return a new instance of {@link IRspEngineHandler} that acts as a handler of the RSP engine
     * @throws DivideInvalidInputException when the query registration URL is no valid URL
     */
    public static IRspEngineHandler createInstance(RspQueryLanguage rspQueryLanguage,
                                                   String url)
            throws DivideInvalidInputException {
        return new RspEngineHandler(rspQueryLanguage, url);
    }

}
