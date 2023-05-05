package be.ugent.idlab.divide.rsp.api;

import be.ugent.idlab.divide.core.exception.DivideInvalidInputException;
import com.google.gson.JsonObject;

import java.util.List;

/**
 * Class capable of registering queries to a wrapped registration URL,
 * and unregistering queries from it.
 */
@SuppressWarnings("UnusedReturnValue")
public interface IRspEngineApiManager {

    /**
     * Registers a query to the RSP engine registration URL of this API manager.
     *
     * @param queryName name of the query to be registered
     * @param queryBody body of the query to be registered
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
    JsonObject registerQuery(String queryName, String queryBody) throws
            RspEngineApiNetworkException,
            RspEngineApiInputException,
            RspEngineApiResponseException,
            DivideInvalidInputException;

    /**
     * Unregisters a query via the RSP engine registration URL of this API manager.
     *
     * @param queryName name of the query to be unregistered
     * @throws RspEngineApiNetworkException when a network error occurs during unregistering
     *                                      the query, causing the query to be not correctly
     *                                      registered at the RSP engine
     * @throws RspEngineApiResponseException when unregistering the query at the RSP engine
     *                                       server fails (HTTP status code is not 2xx)
     * @throws DivideInvalidInputException when the URL at which the query should be unregistered
     *                                     appears to be invalid and no request can therefore be made
     */
    void unregisterQuery(String queryName) throws
            RspEngineApiNetworkException,
            RspEngineApiResponseException,
            DivideInvalidInputException;

    List<JsonObject> getQueryObservers(String queryName) throws
            RspEngineApiNetworkException,
            RspEngineApiResponseException,
            DivideInvalidInputException;

    void registerQueryObserver(String queryName, String observerUrl) throws
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

    /**
     * Registers a stream at the RSP engine.
     *
     * @param streamName name (URI) of the registered stream
     * @param ignoreAlreadyExists when set to true, a {@link RspEngineApiResponseException} will not
     *                            be thrown if the HTTP response code is 400, since this indicates
     *                            that the stream already exists
     * @throws RspEngineApiNetworkException when a network error occurs during registering the stream,
     *                                      causing the streams to be not correctly registered
     * @throws RspEngineApiResponseException when registering the stream at the RSP engine
     *                                       server fails (HTTP status code is not 2xx) - no exception
     *                                       is thrown with a 400 response code if the ignoreAlreadyExists
     *                                       param is set to true
     * @throws DivideInvalidInputException when the URL at which the stream should be registered
     *                                     appears to be invalid and no request can therefore be made
     */
    void registerStream(String streamName, boolean ignoreAlreadyExists) throws
            RspEngineApiNetworkException,
            RspEngineApiResponseException,
            DivideInvalidInputException;

    /**
     * Retrieves all queries (their bodies) that are registered at the RSP engine
     * and that have the specified stream name as one of its input streams.
     *
     * @param streamName name (URI) of the stream that should be one of the input streams of the
     *                   returned queries
     * @return list of query bodies that are registered at the RSP engine and that have the specified
     *         stream name as one of its input stream
     * @throws RspEngineApiNetworkException when a network error occurs during retrieving the queries
     * @throws RspEngineApiResponseException when retrieving the queries at the RSP engine
     *                                       server fails (HTTP status code is not 2xx)
     * @throws DivideInvalidInputException when the URL at which the queries should be retrieved
     *                                     appears to be invalid and no request can therefore be made
     */
    List<JsonObject> retrieveQueriesWithInputStream(String streamName) throws
            RspEngineApiNetworkException,
            RspEngineApiResponseException,
            DivideInvalidInputException;

    /**
     * Enables the forwarding of data sent to the given stream of RSP engine towards the
     * specified WebSocket server URL.
     *
     * @param streamName name (URI) of the stream for which the incoming data should be forwarded
     * @param webSocketUrl URL of the WebSocket server to which the RSP engine should connect to
     *                     forward the incoming stream data
     * @throws RspEngineApiNetworkException when a network error occurs during the enabling of the
     *                                      forwarding, causing the forwarding to be not correctly
     *                                      enabled
     * @throws RspEngineApiResponseException when enabling the forwarding for the given stream at the
     *                                       RSP engine server fails (HTTP status code is not 2xx)
     * @throws DivideInvalidInputException when the URL at which the streams forwarding should be enabled
     *                                     appears to be invalid and no request can therefore be made
     */
    void enableStreamForwardingToWebSocket(String streamName, String webSocketUrl) throws
            RspEngineApiNetworkException,
            RspEngineApiResponseException,
            DivideInvalidInputException;

    /**
     * Disables the forwarding of data sent to the given stream of RSP engine towards the
     * specified WebSocket server URL.
     *
     * @param streamName name (URI) of the stream for which the incoming data should no longer
     *                   be forwarded
     * @param webSocketUrl URL of the WebSocket server from which the RSP engine should disconnect
     *                     to stop forwarding the incoming stream data
     * @throws RspEngineApiNetworkException when a network error occurs during the disabling of the
     *                                      forwarding, causing the forwarding to be not correctly
     *                                      disabled
     * @throws RspEngineApiResponseException when disabling the forwarding for the given stream at the
     *                                       RSP engine server fails (HTTP status code is not 2xx)
     * @throws DivideInvalidInputException when the URL at which the streams forwarding should be disabled
     *                                     appears to be invalid and no request can therefore be made
     */
    void disableStreamForwardingToWebSocket(String streamName, String webSocketUrl) throws
            RspEngineApiNetworkException,
            RspEngineApiResponseException,
            DivideInvalidInputException;

}
