package be.ugent.idlab.divide.rsp;

import be.ugent.idlab.divide.core.engine.IDivideEngine;
import be.ugent.idlab.divide.core.exception.DivideInvalidInputException;

public class RspEngineHandlerFactory {

    /**
     * Creates an {@link IRspEngineHandler} object for a local RSP engine
     * with the given query language and registration URL.
     * @param localRspEngineQueryLanguage query language used by the local RSP engine
     * @param localRspEngineUrl base URL which will be used for communication with the local RSP engine
     * @return a new instance of {@link IRspEngineHandler} that acts as a handler of the local RSP engine
     * @throws DivideInvalidInputException when the query registration URL is no valid URL
     */
    public static IRspEngineHandler createInstance(RspQueryLanguage localRspEngineQueryLanguage,
                                                   String localRspEngineUrl,
                                                   int localRspEngineServerPort,
                                                   String componentId,
                                                   IDivideEngine divideEngine)
            throws DivideInvalidInputException {
        return new RspEngineHandler(
                localRspEngineQueryLanguage, localRspEngineUrl, localRspEngineServerPort,
                componentId, divideEngine);
    }

}
