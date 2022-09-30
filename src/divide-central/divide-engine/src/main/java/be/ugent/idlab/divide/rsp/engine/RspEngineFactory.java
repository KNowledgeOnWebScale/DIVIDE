package be.ugent.idlab.divide.rsp.engine;

import be.ugent.idlab.divide.rsp.RspQueryLanguage;

public class RspEngineFactory {

    /**
     * Creates and returns a new RSP engine with the given parameters.
     *
     * @param rspQueryLanguage query language used by the new RSP engine
     * @param url base URL for communication with the new RSP engine
     * @return newly created RSP engine
     */
    public static IRspEngine createInstance(RspQueryLanguage rspQueryLanguage,
                                            String url) {
        return new RspEngine(rspQueryLanguage, url);
    }

}
