package be.ugent.idlab.divide.rsp;

import be.ugent.idlab.divide.core.exception.DivideInvalidInputException;
import be.ugent.idlab.divide.core.query.IDivideQuery;
import be.ugent.idlab.divide.rsp.engine.IRspEngine;

/**
 * Handler of an RSP engine within to DIVIDE.
 * Wraps the RSP engine content and allows to schedule new queries for registration,
 * as well as updating the registration based on these scheduled queries.
 */
public interface IRspEngineHandler {

    /**
     * Gets the wrapped RSP engine instance.
     * @return the wrapped RSP engine this object is the handler of
     */
    IRspEngine getLocalRspEngine();

    IRspEngine getCentralRspEngine();

    void configureCentralRspEngine(String centralRspEngineUrl,
                                   RspQueryLanguage centralRspEngineQueryLanguage,
                                   String centralRspEngineWebSocketStreamUrl)
            throws DivideInvalidInputException;

    /**
     * Clears list of queries that are scheduled for registration on the
     * wrapped RSP engine.
     */
    void clearRegistrationSchedule();

    /**
     * Clears list of queries that are scheduled for registration on the
     * wrapped RSP engine, but only those that originate from the given
     * DIVIDE query.
     *
     * @param divideQuery DIVIDE query for which the associated scheduled
     *                    RSP queries should be removed from the scheduled list
     */
    void clearRegistrationSchedule(IDivideQuery divideQuery);

    /**
     * Schedules a specific query for registration on the wrapped RSP engine.
     * The query will not be registered yet, but kept track of so it can be
     * registered during the next call of {@link #updateRegistration()}.
     *
     * @param rspQLQueryBody query body to be scheduled for registration, in RSP-QL format
     * @param divideQuery DIVIDE query that was instantiated into the new RSP-QL query body
     */
    void scheduleForRegistration(String rspQLQueryBody, IDivideQuery divideQuery);

    /**
     * Query updating routine which updates the queries registered on the wrapped RSP engine.
     * To do so, it compares the currently registered queries with the queries that were
     * scheduled for registration (with the {@link #scheduleForRegistration(String, IDivideQuery)}
     * method) since the last call of this method. Registered queries that are not again scheduled
     * for registration are unregistered from the RSP engine. Queries scheduled for
     * registration that are not yet registered are registered on the RSP engine.
     */
    void updateRegistration();

    /**
     * Query updating routine which updates the queries registered on the wrapped RSP engine,
     * but only those originating from the given DIVIDE query.
     * To do so, it compares the currently registered queries originating from this DIVIDE query,
     * with the queries that were scheduled for registration, also originating from this DIVIDE
     * query (with the {@link #scheduleForRegistration(String, IDivideQuery)} method) since the
     * last call of this method. Registered queries that are not again scheduled
     * for registration are unregistered from the RSP engine. Queries scheduled for
     * registration that are not yet registered are registered on the RSP engine.
     *
     * @param divideQuery DIVIDE query for which the registration should be specifically updated
     */
    void updateRegistration(IDivideQuery divideQuery);

    /**
     * Unregisters all queries from the wrapped RSP engine that are currently registered
     * via the DIVIDE query derivation.
     * Should be called when this component is removed from the DIVIDE engine.
     */
    void unregisterAllQueries();

    /**
     * Unregisters the queries from the wrapped RSP engine that are currently registered
     * via the DIVIDE query derivation of the specified DIVIDE query.
     * Should be called when the particular DIVIDE query is removed from the DIVIDE engine.
     *
     * @param query DIVIDE query of which the associated RSP engine queries need to be
     *              unregistered from the wrapped RSP engine
     */
    void unregisterAllQueriesOriginatingFromDivideQuery(IDivideQuery query);

    void moveQueriesOriginatingFromDivideQueryCentrally(IDivideQuery query)
            throws RspEngineHandlerException;

    void moveQueriesOriginatingFromDivideQueryLocally(IDivideQuery query)
            throws RspEngineHandlerException;

    /**
     * Enqueues a pause request for the streams of the wrapped RSP engine.
     * This pause HTTP request will ask the RSP engine to temporarily stop sending
     * incoming stream events onto the internal RDF streams of the RSP engine that
     * are used by the continuous queries during evaluation. Incoming stream events
     * will instead be temporarily buffered, until the streams of the RSP engine are
     * restarted by a restart request which can be enqueued via
     * {@link #restartRspEngineStreams()}.
     * The pause request is only actually sent to the RSP engine if the streams are
     * not yet paused.
     */
    void pauseRspEngineStreams();

    /**
     * Enqueues a restart request for the streams of the wrapped RSP engine.
     * This restart HTTP request will ask the RSP engine to restart sending incoming
     * stream events onto the internal RDF streams of the RSP engine that are used by
     * the continuous queries during evaluation. This means that all temporarily
     * buffered data during the pause period will be put on the internal RDF stream
     * as well immediately after the restart.
     * The restart request is only actually sent to the RSP engine if the streams are
     * paused at the start time of executing the queued request.
     */
    void restartRspEngineStreams();

    /**
     * Stops sending any updates (pause or restart requests) for the streams of the
     * wrapped RSP engine. This means that all enqueued requests are cancelled and no
     * new requests can be enqueued anymore; only the currently executing request will
     * still finish its task.
     */
    void stopRspEngineStreamsUpdates();

}
