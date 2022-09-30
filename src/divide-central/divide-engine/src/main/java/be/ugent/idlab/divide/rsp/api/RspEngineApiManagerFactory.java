package be.ugent.idlab.divide.rsp.api;

import be.ugent.idlab.divide.core.exception.DivideInvalidInputException;
import be.ugent.idlab.divide.rsp.engine.IRspEngine;

public class RspEngineApiManagerFactory {

    /**
     * Creates an {@link IRspEngineApiManager} for the given RSP engine
     * @param rspEngine RSP engine for which this API manager exists
     * @return an {@link IRspEngineApiManager} for the given RSP engine
     * @throws DivideInvalidInputException when the RSP engine has an invalid base URL
     */
    public static IRspEngineApiManager createInstance(IRspEngine rspEngine)
            throws DivideInvalidInputException {
        return new RspEngineApiManager(rspEngine);
    }

}
