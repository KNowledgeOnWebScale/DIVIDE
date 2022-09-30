package be.ugent.idlab.divide.rsp.api;

import be.ugent.idlab.divide.core.exception.DivideInvalidInputException;
import be.ugent.idlab.divide.rsp.query.IRspQuery;

/**
 * Class capable of registering queries to a wrapped registration URL,
 * and unregistering queries from it.
 */
public interface IRspEngineApiManager {

    /**
     * Registers a query to the RSP engine registration URL of this API manager.
     *
     * @param query query to be registered
     * @throws RspEngineApiNetworkException when a network error occurs during the registration
     *                                      of the query, causing the query to be not correctly
     *                                      registered at the RSP engine
     * @throws RspEngineApiInputException when the query body cannot be properly encoded into a
     *                                    HTTP request for registration at the engine, causing the
     *                                    query to be not correctly registered at the RSP engine
     * @throws RspEngineApiResponseException when unregistering the query at the RSP engine
     *                                       server fails (HTTP status code is not 2xx)
     * @throws DivideInvalidInputException when the URL to which the query should be registered
     *                                     appears to be invalid and no request can therefore be made
     */
    void registerQuery(IRspQuery query) throws
            RspEngineApiNetworkException,
            RspEngineApiInputException,
            RspEngineApiResponseException,
            DivideInvalidInputException;

    /**
     * Unregisters a query via the RSP engine registration URL of this API manager.
     *
     * @param query query to be unregistered
     * @throws RspEngineApiNetworkException when a network error occurs during unregistering
     *                                      the query, causing the query to be not correctly
     *                                      registered at the RSP engine
     * @throws RspEngineApiResponseException when unregistering the query at the RSP engine
     *                                       server fails (HTTP status code is not 2xx)
     * @throws DivideInvalidInputException when the URL at which the query should be unregistered
     *                                     appears to be invalid and no request can therefore be made
     */
    void unregisterQuery(IRspQuery query) throws
            RspEngineApiNetworkException,
            RspEngineApiResponseException,
            DivideInvalidInputException;

    /**
     * Pauses the streams of the RSP engine via the RSP engine streams URL of
     * this query API manager.
     *
     * @throws RspEngineApiNetworkException when a network error occurs during pausing the streams,
     *                                      causing the streams to be not correctly paused
     * @throws RspEngineApiResponseException when pausing the streams at the RSP engine
     *                                       server fails (HTTP status code is not 2xx)
     * @throws DivideInvalidInputException when the URL at which the streams should be paused
     *                                     appears to be invalid and no request can therefore be made
     */
    void pauseRspEngineStreams() throws
            RspEngineApiNetworkException,
            RspEngineApiResponseException,
            DivideInvalidInputException;

    /**
     * Restarts the streams of the RSP engine via the RSP engine streams URL of
     * this query API manager.
     *
     * @throws RspEngineApiNetworkException when a network error occurs during restarting the streams,
     *                                      causing the streams to be not correctly restarted
     * @throws RspEngineApiResponseException when restarting the streams at the RSP engine
     *                                       server fails (HTTP status code is not 2xx)
     * @throws DivideInvalidInputException when the URL at which the streams should be restarted
     *                                     appears to be invalid and no request can therefore be made
     */
    void restartRspEngineStreams() throws
            RspEngineApiNetworkException,
            RspEngineApiResponseException,
            DivideInvalidInputException;

}
