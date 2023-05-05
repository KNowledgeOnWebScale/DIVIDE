package be.ugent.idlab.divide.rsp;

import be.ugent.idlab.divide.core.component.IComponent;
import be.ugent.idlab.divide.core.engine.IDivideEngine;
import be.ugent.idlab.divide.core.exception.DivideInvalidInputException;
import be.ugent.idlab.divide.core.exception.DivideNotInitializedException;
import be.ugent.idlab.divide.core.query.IDivideQuery;
import be.ugent.idlab.divide.rsp.api.IRspEngineApiManager;
import be.ugent.idlab.divide.rsp.api.RspEngineApiInputException;
import be.ugent.idlab.divide.rsp.api.RspEngineApiManagerFactory;
import be.ugent.idlab.divide.rsp.api.RspEngineApiNetworkException;
import be.ugent.idlab.divide.rsp.api.RspEngineApiResponseException;
import be.ugent.idlab.divide.rsp.engine.IRspEngine;
import be.ugent.idlab.divide.rsp.engine.RspEngineFactory;
import be.ugent.idlab.divide.rsp.query.IRspQuery;
import be.ugent.idlab.divide.rsp.query.RspQueryFactory;
import be.ugent.idlab.divide.rsp.query.window.IStreamWindow;
import be.ugent.idlab.divide.rsp.query.window.StreamWindowFactory;
import be.ugent.idlab.divide.rsp.translate.IQueryTranslator;
import be.ugent.idlab.divide.rsp.translate.QueryTranslatorFactory;
import be.ugent.idlab.divide.util.Constants;
import be.ugent.idlab.util.io.IOUtilities;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.jena.atlas.lib.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class RspEngineHandler implements IRspEngineHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(RspEngineHandler.class.getName());

    /**
     * ID of this RSP engine handler
     */
    private final String id;

    /**
     * List of queries to be scheduled at the RSP engines (contains the
     * RSP queries themselves, the location where they should be scheduled
     * is defined by the queryLocationMap)
     */
    private final List<IRspQuery> scheduledQueries;

    /**
     * Map keeping track of current RSP location where all queries derived
     * from each DIVIDE query are scheduled or should be scheduled next
     */
    private final Map<String, RspLocation> queryLocationMap;

    /**
     * Reference to the local RSP engine
     */
    private final IRspEngine localRspEngine;
    /**
     * Reference to the API manager of the local RSP engine, used to perform
     * different actions on the RSP engine via the API endpoints
     */
    private final IRspEngineApiManager localRspEngineApiManager;
    /**
     * Query translator for the local RSP engine, allowing to translate
     * RSP-QL queries into the RSP query language of that engine
     */
    private final IQueryTranslator localQueryTranslator;

    /**
     * Reference to the local RSP engine
     */
    private IRspEngine centralRspEngine;
    /**
     * Reference to the API manager of the local RSP engine, used to perform
     * different actions on the RSP engine via the API endpoints
     */
    private IRspEngineApiManager centralRspEngineApiManager;
    /**
     * Query translator for the local RSP engine, allowing to translate
     * RSP-QL queries into the RSP query language of that engine
     */
    private IQueryTranslator centralQueryTranslator;

    /**
     * Query counter that ensures that all queries registered at the local
     * and central RSP engines have a unique name (consisting of a unique query
     * number and the ID of this RSP engine handler). There is only one counter
     * for both the local and central RSP engine; this could be separated but
     * there is no need to.
     */
    private long queryCounter;

    /**
     * Status handler for the local RSP engine (used to pause & restart the streams
     * at this RSP engine). There is no status handler for the central RSP engine because
     * this is not required: if a local stream is paused, no data is forwarded to the
     * central RSP engine as well, virtually pausing it as well.
     */
    private final RspEngineStatusHandler localRspEngineStatusHandler;

    /**
     * Map translating local stream URI (used as input streams in the RSP-QL queries)
     * to a unique stream URI for the central RSP engine
     */
    private final Map<String, String> localToCentralStreamUriTranslationMap;

    /**
     * Scheduled executor used to retry query registrations that failed because
     * of a network error
     */
    private ScheduledThreadPoolExecutor retrialScheduledExecutor;
    /**
     * Future object used for executing retrials of query registration updates
     */
    private Future<?> retrialFuture;
    /**
     * Guard that ensures synchronization across thread for accessing the
     * retrial-related objects of this class
     */
    private final Boolean retrialFutureGuard = false;

    private final IDivideEngine divideEngine;

    private final String componentId;


    // CONSTRUCTION, GETTERS & SETTERS

    RspEngineHandler(RspQueryLanguage localRspEngineQueryLanguage,
                     String localRspEngineUrl,
                     int localRspEngineServerPort,
                     String componentId,
                     IDivideEngine divideEngine) throws DivideInvalidInputException {
        // generate ID for this RSP engine handler
        this.id = generateAlphabeticId();

        // initialize the query location map
        this.queryLocationMap = new HashMap<>();

        // make sure trailing '/' is removed from registration url
        if (localRspEngineUrl.endsWith("/")) {
            localRspEngineUrl = localRspEngineUrl.substring(
                    0, localRspEngineUrl.length() - 1);
        }

        // initialize the local RSP engine, its API manager and its query translator
        this.localRspEngine = RspEngineFactory.createInstance(
                localRspEngineQueryLanguage, localRspEngineUrl, localRspEngineServerPort, componentId);
        this.localRspEngineApiManager =
                RspEngineApiManagerFactory.createInstance(this.localRspEngine);
        this.localQueryTranslator =
                QueryTranslatorFactory.createInstance(localRspEngineQueryLanguage);

        // for now, no central RSP engine is configured yet
        this.centralRspEngine = null;
        this.centralRspEngineApiManager = null;
        this.centralQueryTranslator = null;

        // overall counter to manage the query IDs
        // (could be separated for the local vs. central engine, but there is no need to)
        this.queryCounter = 0;

        // list of queries to be scheduled
        // -> can be a single list for both the central and local engine: by the nature
        //    of the system, it is not allowed (and should also be impossible) to update
        //    the location of the derived queries while this list is being filled by a
        //    DIVIDE query derivation task: after completion of such a task, the list should
        //    be emptied (either by registering, either by clearing)
        this.scheduledQueries = new ArrayList<>();

        // create an RSP engine status handler: only required for local RSP engine
        // (status changes in local RSP engine will automatically reflect in central RSP engine)
        this.localRspEngineStatusHandler =
                new RspEngineStatusHandler(localRspEngine, localRspEngineApiManager);

        // initialize an empty map for the translation of local to central stream URIs
        this.localToCentralStreamUriTranslationMap = new HashMap<>();

        // create retrial executor
        this.retrialScheduledExecutor = (ScheduledThreadPoolExecutor)
                Executors.newScheduledThreadPool(1);
        this.retrialScheduledExecutor.setRemoveOnCancelPolicy(true);

        // keep reference to DIVIDE engine
        this.divideEngine = divideEngine;

        // save ID of component for which this RSP engine handler operates
        this.componentId = componentId;
    }

    @Override
    public IRspEngine getLocalRspEngine() {
        return localRspEngine;
    }

    @Override
    public IRspEngine getCentralRspEngine() {
        return centralRspEngine;
    }

    @Override
    public synchronized void configureCentralRspEngine(String centralRspEngineUrl,
                                                       RspQueryLanguage centralRspEngineQueryLanguage,
                                                       String centralRspEngineWebSocketStreamUrl)
            throws DivideInvalidInputException {
        // make sure trailing '/' is removed from registration url
        if (centralRspEngineUrl.endsWith("/")) {
            centralRspEngineUrl = centralRspEngineUrl.substring(
                    0, centralRspEngineUrl.length() - 1);
        }

        // extract server port from URL string
        int centralRspEngineServerPort;
        try {
            centralRspEngineServerPort = new URL(centralRspEngineUrl).getPort();
        } catch (MalformedURLException e) {
            throw new DivideInvalidInputException("Central RSP engine URL is no valid URL");
        }

        // create instances of engine, API manager and translator
        this.centralRspEngine = RspEngineFactory.createInstance(
                centralRspEngineQueryLanguage, centralRspEngineUrl, centralRspEngineServerPort, "central");
        this.centralRspEngine.setWebSocketStreamUrl(centralRspEngineWebSocketStreamUrl);
        this.centralRspEngineApiManager =
                RspEngineApiManagerFactory.createInstance(this.centralRspEngine);
        this.centralQueryTranslator =
                QueryTranslatorFactory.createInstance(centralRspEngineQueryLanguage);
    }



    // MANAGEMENT OF DIVIDE QUERY LOCATION

    private synchronized RspLocation getLocationOfDivideQuery(IDivideQuery divideQuery) {
        // if no location is registered yet for the given DIVIDE query,
        // add the query to the map and set the default location to local
        if (!this.queryLocationMap.containsKey(divideQuery.getName())) {
            updateLocationOfDivideQuery(divideQuery, RspLocation.LOCAL);
        }

        // return the location registered in the map
        return this.queryLocationMap.get(divideQuery.getName());
    }

    private synchronized void updateLocationOfDivideQuery(IDivideQuery divideQuery,
                                                          RspLocation rspLocation) {
        // simply set the location: if no location exists yet, it will be added to the map;
        // otherwise the current location is overwritten
        this.queryLocationMap.put(divideQuery.getName(), rspLocation);

        // update deployment of DIVIDE query in DIVIDE meta model
        try {
            divideEngine.getDivideMetaModel().updateDivideQueryDeployment(
                    divideQuery, divideEngine.getRegisteredComponentById(componentId), rspLocation);
        } catch (DivideNotInitializedException e) {
            // will not happen, DIVIDE will always be initialized at this point
            throw new RuntimeException(e);
        }
    }



    // MANAGEMENT OF STREAM MAPPING FROM LOCAL TO CENTRAL RSP ENGINE

    private static final Pattern RSP_QL_FROM_NAMED_WINDOW_PATTERN = Pattern.compile(
            "\\s*FROM\\s+NAMED\\s+WINDOW\\s+(\\S+)\\s+ON\\s+(\\S+)\\s+\\[([^\\[\\]]+)]",
            Pattern.CASE_INSENSITIVE);

    private synchronized Pair<String, List<String>> updateStreamUrisForCentralRspEngineInQuery(
            String rspQLQueryBody) {
        // look for all named window patterns (i.e., input stream definitions) in the RSP-QL body
        Matcher matcher = RSP_QL_FROM_NAMED_WINDOW_PATTERN.matcher(rspQLQueryBody);
        List<String> localStreamUris = new ArrayList<>();
        while (matcher.find()) {
            // retrieve stream name (and remove the '<' and '>')
            String localStreamUri = matcher.group(2);
            localStreamUri = localStreamUri.substring(1, localStreamUri.length() - 1);

            // save the stream name to the results
            localStreamUris.add(localStreamUri);

            // translate stream name for central engine
            String centralStreamUri = getLocalToCentralStreamUriTranslation(localStreamUri);

            // replace the full pattern (but make sure no other parts are replaced)
            String fullNamedWindowPattern = matcher.group();
            String updatedNamedWindowPattern =
                    fullNamedWindowPattern.replace(localStreamUri, centralStreamUri);
            rspQLQueryBody = rspQLQueryBody.replace(fullNamedWindowPattern, updatedNamedWindowPattern);
        }

        return Pair.create(rspQLQueryBody, localStreamUris);
    }

    private synchronized String getLocalToCentralStreamUriTranslation(String localStreamUri) {
        // first check if the map already contains a translation for this stream URI
        if (this.localToCentralStreamUriTranslationMap.containsKey(localStreamUri)) {
            // -> if yes: return this translation
            return this.localToCentralStreamUriTranslationMap.get(localStreamUri);
        } else {
            // -> if no: do the translation, save if to the map and return it
            String centralStreamUri = translateLocalToCentralStreamUri(localStreamUri);
            this.localToCentralStreamUriTranslationMap.put(localStreamUri, centralStreamUri);
            return centralStreamUri;
        }
    }

    private synchronized String translateLocalToCentralStreamUri(String localStreamUri) {
        // create a unique ID: append existing local stream URI with the ID of this
        // RSP engine, and a unique ID for the given stream
        return String.format("%s/%s/%s",
                localStreamUri.endsWith("/") ?
                        localStreamUri.substring(0, localStreamUri.length() - 1) :
                        localStreamUri,
                id, generateAlphabeticId(10));
    }

    private synchronized Pair<String, List<String>> updateStreamUrisForLocalRspEngineInQuery(
            String rspQLQueryBody) throws RspEngineHandlerException {
        // look for all named window patterns (i.e., input stream definitions) in the RSP-QL body
        Matcher matcher = RSP_QL_FROM_NAMED_WINDOW_PATTERN.matcher(rspQLQueryBody);
        List<String> localStreamUris = new ArrayList<>();
        while (matcher.find()) {
            // retrieve stream name (and remove the '<' and '>')
            String centralStreamUri = matcher.group(2);
            centralStreamUri = centralStreamUri.substring(1, centralStreamUri.length() - 1);

            // translate stream name for central engine
            String localStreamUri = getCentralToLocalStreamUriTranslation(centralStreamUri);

            // save the stream name to the results
            localStreamUris.add(localStreamUri);

            // replace the full pattern (but make sure no other parts are replaced)
            String fullNamedWindowPattern = matcher.group();
            String updatedNamedWindowPattern =
                    fullNamedWindowPattern.replace(centralStreamUri, localStreamUri);
            rspQLQueryBody = rspQLQueryBody.replace(fullNamedWindowPattern, updatedNamedWindowPattern);
        }

        return Pair.create(rspQLQueryBody, localStreamUris);
    }

    private synchronized String getCentralToLocalStreamUriTranslation(String centralStreamUri)
            throws RspEngineHandlerException {
        // loop over all entries in the translation map to find the
        // central stream URI in any of the translations
        for (Map.Entry<String, String> entry :
                this.localToCentralStreamUriTranslationMap.entrySet()) {
            if (centralStreamUri.equals(entry.getValue())) {
                return entry.getKey();
            }
        }

        // if no match in the translations is found, throw an exception:
        // this method should only be called when the central stream URI is
        // already present as a translation
        throw new RspEngineHandlerException(String.format("Illegal state: local stream URI " +
                "corresponding to translated central stream URI %s is retrieved while this central " +
                "stream URI does not exist in the translation map", centralStreamUri));
    }



    // UPDATING REGISTRATION SCHEDULE FROM DIVIDE QUERY DERIVATION TASKS

    @Override
    public synchronized void clearRegistrationSchedule() {
        LOGGER.info("Clearing all queries scheduled for registration");

        this.scheduledQueries.clear();
    }

    @Override
    public synchronized void clearRegistrationSchedule(IDivideQuery divideQuery) {
        LOGGER.info("Clearing all queries scheduled for registration that " +
                "originate from the DIVIDE query '{}'", divideQuery.getName());

        this.scheduledQueries.removeIf(
                rspQuery -> rspQuery.getOriginalDivideQuery().equals(divideQuery));
    }

    @Override
    public synchronized void scheduleForRegistration(String rspQLQueryBody,
                                                     IDivideQuery divideQuery) {
        // create unique query name
        String queryName = String.format("Q%d%s", this.queryCounter++, this.id);

        // retrieve current RSP engine location to where query should be registered
        RspLocation rspLocation = getLocationOfDivideQuery(divideQuery);

        // if the current location to register the queries at is the central RSP engine,
        // the local stream URIs in the RSP-QL query body should be updated to the
        // corresponding stream URIs of the central RSP engine
        // (at this point, it is guaranteed that the central stream URIs are already
        //  registered at the central RSP engine, since this always happens in the same
        //  synchronized blocks as when the RSP location for a DIVIDE query is updated)
        if (rspLocation == RspLocation.CENTRAL) {
            rspQLQueryBody = updateStreamUrisForCentralRspEngineInQuery(rspQLQueryBody).getLeft();
        }

        // retrieving translator and URL of the RSP engine (local or central)
        // on which the query will be registered
        String rspEngineUrl;
        IQueryTranslator translator;
        if (rspLocation == RspLocation.LOCAL) {
            rspEngineUrl = localRspEngine.getBaseUrl();
            translator = localQueryTranslator;
        } else {
            rspEngineUrl = centralRspEngine.getBaseUrl();
            translator = centralQueryTranslator;
        }

        LOGGER.info("Scheduling RSP-QL query with name '{}' for registration at {}: {}",
                queryName,
                rspEngineUrl,
                IOUtilities.removeWhiteSpace(rspQLQueryBody));

        // translate query according to RSP query language of the engine
        // on which it will be registered
        String translatedQueryBody = translator.translateQuery(rspQLQueryBody, queryName);

        // create an RSP query instance
        IRspQuery query = RspQueryFactory.createInstance(
                queryName,
                // preprocess query before registration, to make sure that they appear
                // in a uniform format (since the body is used for comparison between
                // already scheduled and new queries).
                // NOTE: it is no issue if semantically equivalent queries do not match
                //       string-wise - the only consequence then is that this query is
                //       first unregistered and then immediately re-registered by the
                //       updateRegistration method
                preprocessQueryBeforeRegistration(translatedQueryBody),
                preprocessQueryBeforeRegistration(rspQLQueryBody),
                divideQuery);

        // schedule the RSP query for the next registration update
        this.scheduledQueries.add(query);

        LOGGER.info("Query '{}' translated and scheduled for registration at {} with body: {}",
                queryName,
                rspEngineUrl,
                query.getQueryBody());
    }



    // REGISTRATION UPDATING BASED ON REGISTRATION SCHEDULE

    @Override
    public synchronized void updateRegistration(IDivideQuery divideQuery) {
        // retrieve current RSP engine location to where query should be registered
        RspLocation rspLocation = getLocationOfDivideQuery(divideQuery);

        // retrieving RSP engine reference (local or central) on which the
        // query registration will be updated
        IRspEngine rspEngine;
        IRspEngineApiManager rspEngineApiManager;
        if (rspLocation == RspLocation.LOCAL) {
            rspEngine = localRspEngine;
            rspEngineApiManager = localRspEngineApiManager;
        } else {
            rspEngine = centralRspEngine;
            rspEngineApiManager = centralRspEngineApiManager;
        }

        LOGGER.info("Updating RSP engine queries associated to DIVIDE query '{}' at {}",
                divideQuery.getName(), rspEngine.getBaseUrl());

        // obtain currently registered queries in RSP engine
        // and create copy of scheduled queries
        // -> filter both on originating DIVIDE query
        List<IRspQuery> previousQueries = rspEngine.getRegisteredQueries()
                .stream()
                .filter(rspQuery -> rspQuery.getOriginalDivideQuery().equals(divideQuery))
                .collect(Collectors.toList());
        List<IRspQuery> scheduledQueries = this.scheduledQueries
                .stream()
                .filter(rspQuery -> rspQuery.getOriginalDivideQuery().equals(divideQuery))
                .collect(Collectors.toList());

        // perform registration update
        List<Pair<IRspQuery, Boolean>> queriesToRetry = updateRegistration(
                previousQueries, scheduledQueries, rspEngine, rspEngineApiManager);

        // whatever happens, all RSP-QL queries corresponding to the given DIVIDE query
        // should be removed from the list of scheduled queries
        // -> if registration succeeded, the reasons are obvious
        // -> if registration failed, it will only be retried the next time this
        //    method is called, i.e., at the next context update; if the query still
        //    needs to be registered at that point, it will again be the output of
        //    the query derivation and will therefore have been added again to the
        //    list of scheduled queries
        this.scheduledQueries.removeIf(
                rspQuery -> rspQuery.getOriginalDivideQuery().equals(divideQuery));

        // retry queries for which the (un)registering failed due to network issue
        // (but of course only if there are queries to be retried)
        if (!queriesToRetry.isEmpty()) {
            synchronized (this.retrialFutureGuard) {
                LOGGER.info("Query update at {} for DIVIDE query '{}': " +
                                "rescheduling retrial of {} failed queries",
                        rspEngine.getBaseUrl(),
                        divideQuery.getName(),
                        queriesToRetry.size());
                this.retrialFuture = retrialScheduledExecutor.schedule(
                        new QueryRegistrationUpdateRetrialTask(
                                queriesToRetry, 10, rspEngine, rspEngineApiManager),
                        10, TimeUnit.SECONDS);
            }
        } else {
            LOGGER.info("Finished query update for DIVIDE query '{}' at {} - no queries to retry",
                    divideQuery.getName(), rspEngine.getBaseUrl());
        }
    }

    @Override
    public synchronized void updateRegistration() {
        LOGGER.info("Updating RSP engine queries for all DIVIDE queries: handling " +
                "every DIVIDE query separately since the location can be " +
                "different for each DIVIDE query");

        // retrieve all DIVIDE queries for which there is
        // at least 1 query registered at any of the RSP engines
        // and/or at least 1 query scheduled for registration
        Stream<IDivideQuery> divideQueryStream = Stream.concat(
                this.scheduledQueries.stream()
                        .map(IRspQuery::getOriginalDivideQuery),
                this.localRspEngine.getRegisteredQueries().stream()
                        .map(IRspQuery::getOriginalDivideQuery));
        if (this.centralRspEngine != null) {
            divideQueryStream = Stream.concat(
                    divideQueryStream,
                    this.centralRspEngine.getRegisteredQueries().stream()
                            .map(IRspQuery::getOriginalDivideQuery));
        }
        List<IDivideQuery> divideQueries = divideQueryStream
                .distinct()
                .collect(Collectors.toList());

        // trigger the query registration update for every found DIVIDE query
        for (IDivideQuery divideQuery : divideQueries) {
            updateRegistration(divideQuery);
        }
    }

    private synchronized List<Pair<IRspQuery, Boolean>> updateRegistration(
            List<IRspQuery> previousQueries,
            List<IRspQuery> scheduledQueries,
            IRspEngine rspEngine,
            IRspEngineApiManager rspEngineApiManager) {
        // stop all query retrials
        stopQueryUpdateRetrials();

        // create empty list of queries that should be retried
        List<Pair<IRspQuery, Boolean>> queriesToRetry = new ArrayList<>();

        LOGGER.info("Query update at {}: scheduled query names: {} - " +
                        "currently registered query names: {}",
                rspEngine.getBaseUrl(),
                Arrays.toString(scheduledQueries.stream().map(
                        IRspQuery::getQueryName).toArray()),
                Arrays.toString(previousQueries.stream().map(
                        IRspQuery::getQueryName).toArray()));

        // unregister previously valid queries that are no longer valid
        for (IRspQuery previousQuery : previousQueries) {
            // check if the previous query is scheduled again by checking if it is
            // present in the list of scheduled queries
            // -> if it is scheduled again, the returned boolean is true
            boolean scheduledAgain = scheduledQueries.contains(previousQuery);

            // if the query is not scheduled again, it should be unregistered
            // (otherwise, it can be kept registered, and nothing should be done for this query;
            //  except removing it from the scheduled list, which is done in the else clause)
            if (!scheduledAgain) {
                try {
                    // unregister query from RSP engine
                    rspEngineApiManager.unregisterQuery(previousQuery.getQueryName());

                    // only if successful (i.e., if no exception is thrown),
                    // the blueprint of this RSP engine's queries is also updated
                    rspEngine.removeRegisteredQuery(previousQuery);

                    // update meta model after successfully unregistering
                    divideEngine.getDivideMetaModel().removeRegisteredQuery(previousQuery);

                    // if a failure occurs when unregistering this query, the RSP engine's
                    // blueprint of queries is not updated (i.e., this query is not removed
                    // from the list)
                    // => at the following call of this method, this blueprint tells DIVIDE
                    //    that this query is still registered on the engine, and that it
                    //    should again be tried to unregister this query (unless by then it
                    //    is again part of the scheduled queries)

                } catch (RspEngineApiNetworkException e) {
                    LOGGER.error("External network error when unregistering query '{}' at {}",
                            previousQuery.getQueryName(), rspEngine.getBaseUrl());

                    // retrying the request could potentially solve the issue since this is
                    // a network error (i.e., the destination could not be reached)
                    // -> most likely there are network connection issues
                    //    OR the RSP engine server is down
                    queriesToRetry.add(Pair.create(previousQuery, false));

                } catch (RspEngineApiResponseException e) {
                    LOGGER.error("External server error when unregistering query '{}' at {}",
                            previousQuery.getQueryName(), rspEngine.getBaseUrl(), e);

                    // retrying the request is NOT useful, since this is an RSP engine server error
                    // (and the RSP engine server should ensure it can handle the registration
                    //  requests sent by DIVIDE)

                } catch (DivideInvalidInputException e) {
                    // note: this will normally never occur
                    LOGGER.error(Constants.UNKNOWN_ERROR_MARKER,
                            "Internal URL error within DIVIDE when trying to unregister " +
                                    "query '{}' at {}",
                            previousQuery.getQueryName(), rspEngine.getBaseUrl());

                    // retrying the request is NOT useful, since this error represents an
                    // internal condition that will not change
                }

            } else {
                LOGGER.info("Query with name '{}' is still registered as query with name '{}'",
                        scheduledQueries.get(scheduledQueries.indexOf(previousQuery)).getQueryName(),
                        previousQuery.getQueryName());

                // remove query from scheduled queries
                scheduledQueries.remove(previousQuery);
            }
        }

        // register newly valid queries by looping over the list of scheduled queries
        // -> if a scheduled query was already registered before on the RSP engine,
        //    it has already been removed from the list of scheduled queries
        // -> no need for any processing of the remaining items of the scheduled queries
        //    list, they can all simply be registered
        for (IRspQuery query : scheduledQueries) {
            try {
                // register query to RSP engine
                JsonObject jsonQuery = rspEngineApiManager.registerQuery(
                        query.getQueryName(), query.getQueryBody());

                // update RSP query information
                try {
                    updateQueryAfterRegistration(query, jsonQuery, rspEngine,
                            divideEngine.getRegisteredComponentById(componentId));
                } catch (DivideNotInitializedException e) {
                    // will not happen, DIVIDE will always be initialized at this point
                    throw new RuntimeException(e);
                }

                // only if successful (i.e., if no exception is thrown),
                // the blueprint of this RSP engine's queries is also updated
                rspEngine.addRegisteredQuery(query);

                // update meta model after successfully unregistering
                divideEngine.getDivideMetaModel().addRegisteredQuery(query);

                // if a failure occurs when registering this query, the RSP engine's
                // blueprint of queries is not updated (i.e., this query is not removed
                // from the list)
                // => at the following call of this method, this blueprint tells DIVIDE
                //    that this query is not registered yet on the engine; if it is again
                //    part of the scheduled queries, it should then still be registered
                //    (instead of ignoring it since it is already considered registered)

            } catch (RspEngineApiNetworkException e) {
                LOGGER.error("External network error when registering query '{}' at {}",
                        query.getQueryName(), rspEngine.getBaseUrl());

                // retrying the request could potentially solve the issue since this is
                // a network error (i.e., the destination could not be reached)
                // -> most likely there are network connection issues
                //    OR the RSP engine server is down
                queriesToRetry.add(Pair.create(query, true));

            } catch (RspEngineApiResponseException e) {
                LOGGER.error("External server error when registering query '{}' at {}",
                        query.getQueryName(), rspEngine.getBaseUrl(), e);

                // retrying the request is NOT useful, since this is an RSP engine server error
                // (and the RSP engine server should ensure it can handle the registration
                //  requests sent by DIVIDE)

            } catch (RspEngineApiInputException e) {
                // note: DivideInvalidInputException will normally never occur
                LOGGER.error(Constants.UNKNOWN_ERROR_MARKER,
                        "Internal query error within DIVIDE when trying to register query '{}' at {}",
                        query.getQueryName(), rspEngine.getBaseUrl());

                // retrying the request is NOT useful, since this error represents an
                // internal condition that will not change

            } catch (DivideInvalidInputException e) {
                // note: this will normally never occur
                LOGGER.error(Constants.UNKNOWN_ERROR_MARKER,
                        "Internal URL error within DIVIDE when trying to register query '{}' at {}",
                        query.getQueryName(), rspEngine.getBaseUrl());

                // retrying the request is NOT useful, since this error represents an
                // internal condition that will not change
            }
        }

        return queriesToRetry;
    }

    private void updateQueryAfterRegistration(IRspQuery query,
                                              JsonObject jsonQuery,
                                              IRspEngine rspEngine,
                                              IComponent associatedComponent) {
        query.updateAfterRegistration(
                jsonQuery.get("id").getAsString(),
                convertToStreamWindows(jsonQuery.getAsJsonArray("streamWindows")),
                rspEngine,
                associatedComponent);
    }

    private List<IStreamWindow> convertToStreamWindows(JsonArray jsonArrayStreamWindows) {
        List<IStreamWindow> result = new ArrayList<>();
        for (JsonElement jsonElement : jsonArrayStreamWindows) {
            result.add(StreamWindowFactory.createInstance(
                    jsonElement.getAsJsonObject().get("streamIri").getAsString(),
                    jsonElement.getAsJsonObject().get("windowDefinition").getAsString()));
        }
        return result;
    }



    // RETRYING FAILED QUERY REGISTRATION UPDATES IN CASE OF NETWORKING ERRORS

    private class QueryRegistrationUpdateRetrialTask implements Runnable {

        private final Logger LOGGER = LoggerFactory.getLogger(
                QueryRegistrationUpdateRetrialTask.class.getName());

        /**
         * List of pairs representing queries for which the registering or
         * unregistering should be retried. Each pair consists of the query
         * to be registered or unregistered, and a boolean representing
         * whether the required operation is a registration or not
         * (i.e., true means registering, false means unregistering)
         */
        private final List<Pair<IRspQuery, Boolean>> queryPairs;

        /**
         * Time (in seconds) before this batch of queries was rescheduled
         * for a new register/unregister attempt
         */
        private final long delayBeforeRetrial;

        private final IRspEngine rspEngine;
        private final IRspEngineApiManager rspEngineApiManager;

        public QueryRegistrationUpdateRetrialTask(List<Pair<IRspQuery, Boolean>> queryPairs,
                                                  long delayBeforeRetrial,
                                                  IRspEngine rspEngine,
                                                  IRspEngineApiManager rspEngineApiManager) {
            this.queryPairs = queryPairs;
            this.delayBeforeRetrial = delayBeforeRetrial;
            this.rspEngine = rspEngine;
            this.rspEngineApiManager = rspEngineApiManager;
        }

        @Override
        public void run() {
            LOGGER.info("Starting retrial of updating registration at {} for {} queries: {}",
                    rspEngine.getBaseUrl(),
                    queryPairs.size(),
                    Arrays.toString(queryPairs.stream()
                            .map(Pair::getLeft)
                            .map(IRspQuery::getQueryName)
                            .toArray()));

            // create empty list of queries that should be retried
            List<Pair<IRspQuery, Boolean>> queriesToRetry = new ArrayList<>();

            for (Pair<IRspQuery, Boolean> queryBooleanPair : queryPairs) {
                IRspQuery query = queryBooleanPair.getLeft();
                boolean register = queryBooleanPair.getRight();

                LOGGER.info("Query update retrial at {}: retry {} {}",
                        rspEngine.getBaseUrl(),
                        register ? "registering" : "unregistering",
                        query.getQueryName());

                try {
                    // register or unregister query at RSP engine
                    if (register) {
                        JsonObject jsonQuery = rspEngineApiManager.registerQuery(
                                query.getQueryName(), query.getQueryBody());

                        // update RSP query information
                        try {
                            updateQueryAfterRegistration(query, jsonQuery, rspEngine,
                                    divideEngine.getRegisteredComponentById(componentId));
                        } catch (DivideNotInitializedException e) {
                            // will not happen, DIVIDE will always be initialized at this point
                            throw new RuntimeException(e);
                        }

                    } else {
                        rspEngineApiManager.unregisterQuery(query.getQueryName());
                    }

                    // only if successful (i.e., if no exception is thrown),
                    // the blueprint of this RSP engine's queries is also updated
                    rspEngine.addRegisteredQuery(query);

                    // update meta model after successful update
                    if (register) {
                        divideEngine.getDivideMetaModel().addRegisteredQuery(query);
                    } else {
                        divideEngine.getDivideMetaModel().removeRegisteredQuery(query);
                    }

                } catch (RspEngineApiNetworkException e) {
                    LOGGER.error("External network error when registering query '{}' at {}",
                            query.getQueryName(), rspEngine.getBaseUrl());

                    // retry once again if registration failed again
                    queriesToRetry.add(Pair.create(query, register));

                } catch (RspEngineApiResponseException e) {
                    LOGGER.error("External server error when registering query '{}' at {}",
                            query.getQueryName(), rspEngine.getBaseUrl(), e);

                    // retrying the request is NOT useful, since this is an RSP engine server error
                    // (and the RSP engine server should ensure it can handle the registration
                    //  requests sent by DIVIDE)

                } catch (RspEngineApiInputException e) {
                    // note: DivideInvalidInputException will normally never occur
                    LOGGER.error(Constants.UNKNOWN_ERROR_MARKER,
                            "Internal query error within DIVIDE when trying to register query '{}' at {}",
                            query.getQueryName(), rspEngine.getBaseUrl());

                    // retrying the request is NOT useful, since this error represents an
                    // internal condition that will not change

                } catch (DivideInvalidInputException e) {
                    // note: this will normally never occur
                    LOGGER.error(Constants.UNKNOWN_ERROR_MARKER,
                            "Internal URL error within DIVIDE when trying to register query '{}' at {}",
                            query.getQueryName(), rspEngine.getBaseUrl());

                    // retrying the request is NOT useful, since this error represents an
                    // internal condition that will not change
                }

                // if the thread has been interrupted, then stop going over all queries and
                // immediately return (also do not schedule a new retrial task, obviously)
                if (Thread.currentThread().isInterrupted()) {
                    LOGGER.info("Query update retrial at {}: task interrupted after updating {}",
                            rspEngine.getBaseUrl(),
                            query.getQueryName());
                    return;
                }
            }

            if (!queriesToRetry.isEmpty()) {
                synchronized (retrialFutureGuard) {
                    // only reschedule if thread has not been interrupted by now
                    if (!Thread.currentThread().isInterrupted()) {
                        LOGGER.info("Query update retrial at {}: rescheduling retrial " +
                                        "of {} failed queries",
                                rspEngine.getBaseUrl(), queriesToRetry.size());

                        // schedule new retry, with a doubled delay
                        retrialFuture = retrialScheduledExecutor.schedule(
                                new QueryRegistrationUpdateRetrialTask(
                                        queriesToRetry, delayBeforeRetrial * 2,
                                        rspEngine, rspEngineApiManager),
                                delayBeforeRetrial * 2,
                                TimeUnit.SECONDS);

                    } else {
                        LOGGER.info("Query update retrial at {}: NOT rescheduling retrial " +
                                        "of {} failed queries because of interruption",
                                rspEngine.getBaseUrl(), queriesToRetry.size());
                    }
                }
            } else {
                LOGGER.info("Finished query update retrial at {} - no queries to retry",
                        rspEngine.getBaseUrl());
            }
        }

    }

    private void stopQueryUpdateRetrials() {
        // shutdown scheduled executor
        // -> no new tasks can be submitted
        LOGGER.info("Shutting down retrial scheduled executor");
        retrialScheduledExecutor.shutdown();

        LOGGER.info("Trying to cancel the latest retrial task");
        synchronized (retrialFutureGuard) {
            if (retrialFuture != null) {
                // cancel the task, and allow for interruption while running
                // -> if not started yet, it will never start
                //    (a shutdown does not prevent this, so this is required)
                // -> if already started, the thread will be interrupted
                //    (throwing an InterruptedException if sleeping, and otherwise
                //     setting the interrupt flag so that the thread knows it can
                //     finish but should not reschedule a new retrial on failure)
                // -> if already finished, this method will simply return
                LOGGER.info("Canceling the latest retrial task");
                retrialFuture.cancel(true);
                retrialFuture = null;
            }
        }

        // await for termination of tasks of scheduled executor
        // -> the last scheduled future has been canceled, so will not
        //    start if it was not started yet, and will otherwise regularly
        //    check for its interruption and return immediately at anchor point
        try {
            LOGGER.info("Awaiting termination of retrial tasks");
            if (!retrialScheduledExecutor.awaitTermination(1, TimeUnit.MINUTES)) {
                retrialScheduledExecutor.shutdownNow();
                LOGGER.error(Constants.UNKNOWN_ERROR_MARKER,
                        "Awaiting termination not finished after 1 minute => hard shutdown");
            }
        } catch (InterruptedException e) {
            LOGGER.error(Constants.UNKNOWN_ERROR_MARKER,
                    "Interruption while awaiting termination of retrial tasks => hard shutdown");
            retrialScheduledExecutor.shutdownNow();
        }

        // create a new retrial executor
        this.retrialScheduledExecutor = (ScheduledThreadPoolExecutor)
                Executors.newScheduledThreadPool(1);
        this.retrialScheduledExecutor.setRemoveOnCancelPolicy(true);
    }



    // DIVIDE QUERY OR COMPONENT REMOVAL:
    // -> UNREGISTER ALL QUERIES FROM BOTH ENGINES WHEN THE WHOLE COMPONENT IS REMOVED
    // -> ONLY UNREGISTER ALL QUERIES ASSOCIATED TO DIVIDE QUERY WHEN ONLY DIVIDE QUERY IS REMOVED

    @Override
    public void unregisterAllQueries() {
        LOGGER.info("Unregistering all RSP engine queries at {}",
                centralRspEngine != null ?
                        localRspEngine.getBaseUrl() + " and " + centralRspEngine.getBaseUrl()
                        : localRspEngine.getBaseUrl());

        // stop all query update retrials since the associated component will be unregistered
        stopQueryUpdateRetrials();

        // create new list of all registered queries on local RSP engine
        List<IRspQuery> localQueriesToUnregister =
                new ArrayList<>(localRspEngine.getRegisteredQueries());

        if (!localQueriesToUnregister.isEmpty()) {
            LOGGER.info("Unregistering the following queries at {}: {}",
                    localRspEngine.getBaseUrl(),
                    Arrays.toString(localQueriesToUnregister.stream().map(
                            IRspQuery::getQueryName).toArray()));

            // unregister queries
            for (IRspQuery query : localQueriesToUnregister) {
                unregisterQuery(query, localRspEngine, localRspEngineApiManager);
            }

        } else {
            LOGGER.info("No RSP engine queries registered anymore at {}",
                    localRspEngine.getBaseUrl());
        }

        // do the same for the central RSP engine if it is configured
        if (centralRspEngine != null) {
            // create new list of all registered queries on central RSP engine
            List<IRspQuery> centralQueriesToUnregister =
                    new ArrayList<>(centralRspEngine.getRegisteredQueries());

            if (!centralQueriesToUnregister.isEmpty()) {
                LOGGER.info("Unregistering the following queries at {}: {}",
                        centralRspEngine.getBaseUrl(),
                        Arrays.toString(centralQueriesToUnregister.stream().map(
                                IRspQuery::getQueryName).toArray()));

                // unregister queries
                for (IRspQuery query : centralQueriesToUnregister) {
                    unregisterQuery(query, centralRspEngine, centralRspEngineApiManager);
                }

            } else {
                LOGGER.info("No RSP engine queries registered anymore at {}",
                        centralRspEngine.getBaseUrl());
            }
        }
    }

    @Override
    public void unregisterAllQueriesOriginatingFromDivideQuery(IDivideQuery divideQuery) {
        LOGGER.info("Unregistering RSP engine queries of DIVIDE query '{}'",
                divideQuery.getName());

        // stop all query update retrials since otherwise some new queries associated
        // to this removed DIVIDE query might be re-registered
        stopQueryUpdateRetrials();

        // retrieve current RSP engine location to where queries of the
        // given DIVIDE query are be registered
        RspLocation rspLocation = getLocationOfDivideQuery(divideQuery);

        // retrieving RSP engine reference (local or central) on which the
        // query registration will be updated
        IRspEngine rspEngine;
        IRspEngineApiManager rspEngineApiManager;
        if (rspLocation == RspLocation.LOCAL) {
            rspEngine = localRspEngine;
            rspEngineApiManager = localRspEngineApiManager;
        } else {
            rspEngine = centralRspEngine;
            rspEngineApiManager = centralRspEngineApiManager;
        }

        // retrieve list of all queries associated to the given DIVIDE query
        List<IRspQuery> queriesToUnregister = rspEngine.getRegisteredQueries()
                .stream()
                .filter(rspQuery -> divideQuery.equals(rspQuery.getOriginalDivideQuery()))
                .collect(Collectors.toList());

        if (!queriesToUnregister.isEmpty()) {
            LOGGER.info("Unregistering the following queries at {}: {}",
                    rspEngine.getBaseUrl(),
                    Arrays.toString(queriesToUnregister.stream().map(
                            IRspQuery::getQueryName).toArray()));

            // unregister queries
            for (IRspQuery query : queriesToUnregister) {
                unregisterQuery(query, rspEngine, rspEngineApiManager);
            }

        } else {
            LOGGER.info("No RSP engine queries registered at {} that are associated to" +
                            " DIVIDE query '{}'",
                    rspEngine.getBaseUrl(), divideQuery.getName());
        }
    }

    private void unregisterQuery(IRspQuery query,
                                 IRspEngine rspEngine,
                                 IRspEngineApiManager rspEngineApiManager) {
        try {
            // unregister query from RSP engine
            rspEngineApiManager.unregisterQuery(query.getQueryName());

            // only if successful (i.e., if no exception is thrown),
            // the blueprint of this RSP engine's queries is also updated
            rspEngine.removeRegisteredQuery(query);

            // update meta model accordingly
            divideEngine.getDivideMetaModel().removeRegisteredQuery(query);

            // if unregistering fails, it is what it is and it should not be retried

        } catch (RspEngineApiNetworkException e) {
            LOGGER.error("External network error when unregistering query '{}' at {}",
                    query.getQueryName(), rspEngine.getBaseUrl());

        } catch (RspEngineApiResponseException e) {
            LOGGER.error("External server error when unregistering query '{}' at {}",
                    query.getQueryName(), rspEngine.getBaseUrl(), e);

        } catch (DivideInvalidInputException e) {
            // note: this will normally never occur
            LOGGER.error(Constants.UNKNOWN_ERROR_MARKER,
                    "Internal URL error within DIVIDE when trying to unregister " +
                            "query '{}' at {}",
                    query.getQueryName(), rspEngine.getBaseUrl());
        }
    }



    // HANDLING OF RSP ENGINE STREAMS
    // -> independent of query location: if the local stream is paused, no data reaches both the
    //                                   local and central RSP engine streams

    @Override
    public synchronized void pauseRspEngineStreams() {
        LOGGER.info("Pausing streams of RSP engine with base URL {}",
                localRspEngine.getBaseUrl());
        localRspEngineStatusHandler.pauseRspEngine();
    }

    @Override
    public synchronized void restartRspEngineStreams() {
        LOGGER.info("Restarting streams of RSP engine with base URL {}",
                localRspEngine.getBaseUrl());
        localRspEngineStatusHandler.restartRspEngine();
    }

    @Override
    public void stopRspEngineStreamsUpdates() {
        LOGGER.info("Stopping streams updates for RSP engine with base URL {}",
                localRspEngine.getBaseUrl());
        localRspEngineStatusHandler.stopAllTasks();
    }



    // HELPING METHODS

    private String preprocessQueryBeforeRegistration(String query) {
        // preprocess query before registration by removing all unnecessary whitespace
        return IOUtilities.removeWhiteSpace(query).trim();
    }

    private String generateAlphabeticId() {
        return generateAlphabeticId(5);
    }

    private String generateAlphabeticId(int targetStringLength) {
        int leftLimit = 97; // letter 'a'
        int rightLimit = 122; // letter 'z'
        Random random = new Random();

        return random.ints(leftLimit, rightLimit + 1)
                .limit(targetStringLength)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }



    // QUERY LOCATION MOVES BETWEEN LOCAL & CENTRAL RSP ENGINE

    @Override
    public synchronized void moveQueriesOriginatingFromDivideQueryCentrally(IDivideQuery divideQuery)
            throws RspEngineHandlerException {
        LOGGER.info("Moving queries originating from DIVIDE query {} centrally",
                divideQuery.getName());

        // only continue if a central RSP engine is configured: otherwise no moves
        // between local and central can be made
        if (this.centralRspEngine == null) {
            LOGGER.warn("Cannot move queries originating from DIVIDE query '{}' " +
                            "to central RSP engine: no central RSP engine configured yet",
                    divideQuery.getName());
            return;
        }

        // only continue if there are no scheduled queries left corresponding
        // to the given DIVIDE query
        if (this.scheduledQueries.stream().anyMatch(
                iRspQuery -> iRspQuery.getOriginalDivideQuery().equals(divideQuery))) {
            LOGGER.warn("Cannot move queries originating from DIVIDE query '{}' " +
                    "to central RSP engine: some RSP queries corresponding to this " +
                    "DIVIDE query are currently scheduled for registration, but are " +
                    "not registered yet -> illegal state", divideQuery.getName());
            return;
        }

        // check the location for the given DIVIDE query:
        // only continue if it is local (if it already is central, there is no need
        // to move the queries centrally)
        if (getLocationOfDivideQuery(divideQuery) == RspLocation.CENTRAL) {
            LOGGER.warn("Moving queries originating from DIVIDE query '{}' " +
                    "to central RSP engine is not done since they are already registered " +
                    "on the central RSP engine", divideQuery.getName());
            return;
        }

        // stop the query update retrial tasks
        stopQueryUpdateRetrials();

        // retrieve all queries registered on the local RSP engine that originate
        // from the given DIVIDE query
        List<IRspQuery> registeredLocalQueries =
                this.localRspEngine.getRegisteredQueries().stream()
                        .filter(iRspQuery -> iRspQuery.getOriginalDivideQuery().equals(divideQuery))
                        .collect(Collectors.toList());

        // only continue if the list of registered local queries is not empty
        // -> otherwise it makes no sense to trigger a query location update
        if (registeredLocalQueries.isEmpty()) {
            throw new RspEngineHandlerException(
                    String.format("Moving the location of queries associated to DIVIDE query %s" +
                            " makes no sense if currently no queries are actually registered " +
                            "associated to that DIVIDE query", divideQuery.getName()));
        }

        // update the registration for each query
        for (IRspQuery registeredLocalQuery : registeredLocalQueries) {
            LOGGER.info("Updating location of registered local query {}",
                    registeredLocalQuery.getQueryName());

            // update the stream names of the original RSP-QL query body of this query
            Pair<String, List<String>> updatedQueryResult =
                    updateStreamUrisForCentralRspEngineInQuery(
                            registeredLocalQuery.getRspQLQueryBody());
            String updatedRspQlQueryBody = updatedQueryResult.getLeft();
            List<String> inputStreamNames = updatedQueryResult.getRight();

            // create a new query name
            String newQueryName = String.format("Q%d%s", this.queryCounter++, this.id);

            // translate the updated RSP-QL query body for the central engine
            String translatedQueryBody = this.centralQueryTranslator.translateQuery(
                    updatedRspQlQueryBody, newQueryName);
            LOGGER.info("Translated original RSP-QL query body '{}' to new body '{}'",
                    registeredLocalQuery.getRspQLQueryBody(), translatedQueryBody);

            // loop over all input stream names
            for (String localStreamUri : inputStreamNames) {
                // get translation of stream URI for central RSP engine
                // (which is also used in the updated RSP-QL query body)
                String centralStreamUri = getLocalToCentralStreamUriTranslation(localStreamUri);

                LOGGER.info("Updating input stream '{}' of local query {} to '{}': " +
                                "registering new stream to central RSP engine & " +
                                "enabling forwarding of stream on local RSP engine to central RSP engine",
                        localStreamUri, registeredLocalQuery.getQueryName(), centralStreamUri);

                // register this central stream URI to the central RSP engine
                // (no exception will be thrown if the stream already exists)
                try {
                    this.centralRspEngineApiManager.registerStream(centralStreamUri, true);
                } catch (RspEngineApiNetworkException | RspEngineApiResponseException
                         | DivideInvalidInputException e) {
                    throw new RspEngineHandlerException(
                            String.format("The input stream %s could not be registered at the " +
                                    "central RSP engine", centralStreamUri), e);
                }

                // generate WebSocket URL of the central stream URI input WebSocket server
                // of the central RSP engine and register it as forwarding WebSocket to the
                // original stream at the local RSP engine
                try {
                    String webSocketUrl = String.format("%s/streams/%s",
                            this.centralRspEngine.getWebSocketStreamUrl(),
                            URLEncoder.encode(centralStreamUri, StandardCharsets.UTF_8.toString()));

                    this.localRspEngineApiManager.enableStreamForwardingToWebSocket(
                            localStreamUri, webSocketUrl);

                } catch (UnsupportedEncodingException | RspEngineApiNetworkException
                         | RspEngineApiResponseException | DivideInvalidInputException e) {
                    throw new RspEngineHandlerException(
                            String.format("The forwarding of the local stream URI %s to the WebSocket" +
                                    " stream server of the central RSP engine (for central stream %s) " +
                                    "could not be enabled", localStreamUri, centralStreamUri), e);
                }
            }

            // retrieve the observer URLs of the original local query
            List<String> localQueryObservers = new ArrayList<>();
            try {
                List<JsonObject> jsonObservers = localRspEngineApiManager.
                        getQueryObservers(registeredLocalQuery.getQueryName());
                for (JsonObject jsonObserver : jsonObservers) {
                    localQueryObservers.add(jsonObserver.get("observerURL").getAsString());
                }
            } catch (DivideInvalidInputException | RspEngineApiResponseException
                    | RspEngineApiNetworkException e) {
                LOGGER.warn("Could not retrieve observers of original local query {}",
                        registeredLocalQuery.getQueryName(), e);
            }

            try {
                // unregister the original query from the local RSP engine
                localRspEngineApiManager.unregisterQuery(registeredLocalQuery.getQueryName());
                localRspEngine.removeRegisteredQuery(registeredLocalQuery);
                divideEngine.getDivideMetaModel().removeRegisteredQuery(registeredLocalQuery);

            } catch (DivideInvalidInputException | RspEngineApiResponseException
                     | RspEngineApiNetworkException e) {
                throw new RspEngineHandlerException(
                        String.format("The old query with name %s could not be unregistered from the " +
                                "local RSP engine", registeredLocalQuery.getQueryName()), e);
            }

            // create a new RSP query instance for the central RSP query
            IRspQuery newCentralQuery = RspQueryFactory.createInstance(
                    newQueryName,
                    preprocessQueryBeforeRegistration(translatedQueryBody),
                    preprocessQueryBeforeRegistration(updatedRspQlQueryBody),
                    divideQuery);

            try {
                // register the new query to the central RSP engine
                JsonObject jsonQuery = centralRspEngineApiManager.registerQuery(
                        newCentralQuery.getQueryName(), newCentralQuery.getQueryBody());
                try {
                    updateQueryAfterRegistration(newCentralQuery, jsonQuery, centralRspEngine,
                            divideEngine.getRegisteredComponentById(componentId));
                } catch (DivideNotInitializedException e) {
                    // will not happen, DIVIDE will always be initialized at this point
                    throw new RuntimeException(e);
                }
                centralRspEngine.addRegisteredQuery(newCentralQuery);
                divideEngine.getDivideMetaModel().addRegisteredQuery(newCentralQuery);

            } catch (RspEngineApiNetworkException | RspEngineApiResponseException
                     | DivideInvalidInputException | RspEngineApiInputException e) {
                throw new RspEngineHandlerException(
                        String.format("The new query with name %s could not be registered at the " +
                                "central RSP engine", newQueryName), e);
            }

            // retrieve the observer URLs of the newly registered central query
            List<String> centralQueryObservers = new ArrayList<>();
            try {
                List<JsonObject> jsonObservers = centralRspEngineApiManager.
                        getQueryObservers(newCentralQuery.getQueryName());
                for (JsonObject jsonObserver : jsonObservers) {
                    centralQueryObservers.add(jsonObserver.get("observerURL").getAsString());
                }
            } catch (DivideInvalidInputException | RspEngineApiResponseException
                     | RspEngineApiNetworkException e) {
                LOGGER.warn("Could not retrieve observers of new central query {}",
                        newCentralQuery.getQueryName(), e);
            }

            // register the observers of the original local query to the new central query
            for (String localQueryObserver : localQueryObservers) {
                if (centralQueryObservers.contains(localQueryObserver)) {
                    LOGGER.info("Not registering query observer {} to new query {} at central RSP engine: " +
                            "observer already exists", localQueryObserver, newCentralQuery.getQueryName());
                } else {
                    try {
                        centralRspEngineApiManager.registerQueryObserver(
                                newCentralQuery.getQueryName(), localQueryObserver);
                    } catch (RspEngineApiNetworkException | RspEngineApiResponseException
                             | DivideInvalidInputException e) {
                        LOGGER.warn("Could not register observer {} of central query {}",
                                localQueryObserver, newCentralQuery.getQueryName(), e);
                    }
                }
            }
        }

        // update the location for the given DIVIDE query for future query derivations
        updateLocationOfDivideQuery(divideQuery, RspLocation.CENTRAL);
    }

    @Override
    public synchronized void moveQueriesOriginatingFromDivideQueryLocally(IDivideQuery divideQuery)
            throws RspEngineHandlerException {
        // only continue if a central RSP engine is configured: otherwise no moves
        // between local and central can be made
        if (this.centralRspEngine == null) {
            LOGGER.warn("Cannot move queries originating from DIVIDE query '{}' " +
                            "to local RSP engine: no central RSP engine configured yet, " +
                            "so queries will be running on the local RSP engine anyway",
                    divideQuery.getName());
            return;
        }

        // only continue if there are no schedules queries left corresponding
        // to the given DIVIDE query
        if (this.scheduledQueries.stream().anyMatch(
                iRspQuery -> iRspQuery.getOriginalDivideQuery().equals(divideQuery))) {
            LOGGER.warn("Cannot move queries originating from DIVIDE query '{}' " +
                    "to local RSP engine: some RSP queries corresponding to this " +
                    "DIVIDE query are currently scheduled for registration, but are " +
                    "not registered yet -> illegal state", divideQuery.getName());
            return;
        }

        // check the location for the given DIVIDE query:
        // only continue if it is local (if it already is central, there is no need
        // to move the queries centrally)
        if (getLocationOfDivideQuery(divideQuery) == RspLocation.LOCAL) {
            LOGGER.warn("Moving queries originating from DIVIDE query '{}' " +
                    "to local RSP engine is not done since they are already registered " +
                    "on the local RSP engine", divideQuery.getName());
            return;
        }

        // stop the query update retrial tasks
        stopQueryUpdateRetrials();

        // retrieve all queries registered on the central RSP engine that originate
        // from the given DIVIDE query
        List<IRspQuery> registeredCentralQueries =
                this.centralRspEngine.getRegisteredQueries().stream()
                        .filter(iRspQuery -> iRspQuery.getOriginalDivideQuery().equals(divideQuery))
                        .collect(Collectors.toList());

        // keep track of all input streams of the local RSP engine, of which the
        // corresponding input stream of the central RSP engine was used as an input
        // stream in one of the unregistered central RSP engine queries
        List<String> localStreamUrisOfUnregisteredCentralQueries = new ArrayList<>();

        // update the registration for each query
        for (IRspQuery registeredCentralQuery : registeredCentralQueries) {
            LOGGER.info("Updating location of registered central query {}",
                    registeredCentralQuery.getQueryName());

            // update the stream names of the original RSP-QL query body of this central query,
            // back to their state for the local query
            Pair<String, List<String>> updatedQueryResult =
                    updateStreamUrisForLocalRspEngineInQuery(
                            registeredCentralQuery.getRspQLQueryBody());
            String updatedRspQlQueryBody = updatedQueryResult.getLeft();
            List<String> inputStreamNames = updatedQueryResult.getRight();

            // save all stream names to process after updating the query registrations
            localStreamUrisOfUnregisteredCentralQueries.addAll(inputStreamNames);

            // create new query name
            String newQueryName = String.format("Q%d%s", this.queryCounter++, this.id);

            // translate the updated RSP-QL query body for the central engine
            String translatedQueryBody = this.localQueryTranslator.translateQuery(
                    updatedRspQlQueryBody, newQueryName);
            LOGGER.info("Translated original RSP-QL query body '{}' to new body '{}'",
                    registeredCentralQuery.getRspQLQueryBody(), translatedQueryBody);

            // retrieve the observer URLs of the original central query
            List<String> centralQueryObservers = new ArrayList<>();
            try {
                List<JsonObject> jsonObservers = centralRspEngineApiManager.
                        getQueryObservers(registeredCentralQuery.getQueryName());
                for (JsonObject jsonObserver : jsonObservers) {
                    centralQueryObservers.add(jsonObserver.get("observerURL").getAsString());
                }
            } catch (DivideInvalidInputException | RspEngineApiResponseException
                     | RspEngineApiNetworkException e) {
                LOGGER.warn("Could not retrieve observers of original central query {}",
                        registeredCentralQuery.getQueryName(), e);
            }

            try {
                // unregister the original query from the central RSP engine
                centralRspEngineApiManager.unregisterQuery(registeredCentralQuery.getQueryName());
                centralRspEngine.removeRegisteredQuery(registeredCentralQuery);
                divideEngine.getDivideMetaModel().removeRegisteredQuery(registeredCentralQuery);

            } catch (DivideInvalidInputException | RspEngineApiResponseException
                     | RspEngineApiNetworkException e) {
                throw new RspEngineHandlerException(
                        String.format("The old query with name %s could not be unregistered from the " +
                                "central RSP engine", registeredCentralQuery.getQueryName()), e);
            }

            // create a new RSP query instance for the local RSP query
            IRspQuery newLocalQuery = RspQueryFactory.createInstance(
                    newQueryName,
                    preprocessQueryBeforeRegistration(translatedQueryBody),
                    preprocessQueryBeforeRegistration(updatedRspQlQueryBody),
                    divideQuery);

            try {
                // register the new query to the local RSP engine
                JsonObject jsonQuery = localRspEngineApiManager.registerQuery(
                        newLocalQuery.getQueryName(), newLocalQuery.getQueryBody());
                try {
                    updateQueryAfterRegistration(newLocalQuery, jsonQuery, localRspEngine,
                            divideEngine.getRegisteredComponentById(componentId));
                } catch (DivideNotInitializedException e) {
                    // will not happen, DIVIDE will always be initialized at this point
                    throw new RuntimeException(e);
                }
                localRspEngine.addRegisteredQuery(newLocalQuery);
                divideEngine.getDivideMetaModel().addRegisteredQuery(newLocalQuery);

            } catch (RspEngineApiNetworkException | RspEngineApiResponseException
                     | DivideInvalidInputException | RspEngineApiInputException e) {
                throw new RspEngineHandlerException(
                        String.format("The new query with name %s could not be registered at the " +
                                "local RSP engine", newQueryName), e);
            }

            // retrieve the observer URLs of the newly registered local query
            List<String> localQueryObservers = new ArrayList<>();
            try {
                List<JsonObject> jsonObservers = localRspEngineApiManager.
                        getQueryObservers(newLocalQuery.getQueryName());
                for (JsonObject jsonObserver : jsonObservers) {
                    localQueryObservers.add(jsonObserver.get("observerURL").getAsString());
                }
            } catch (DivideInvalidInputException | RspEngineApiResponseException
                     | RspEngineApiNetworkException e) {
                LOGGER.warn("Could not retrieve observers of new local query {}",
                        newLocalQuery.getQueryName(), e);
            }

            // register the observers of the original central query to the new local query
            for (String centralQueryObserver : centralQueryObservers) {
                if (localQueryObservers.contains(centralQueryObserver)) {
                    LOGGER.info("Not registering query observer {} to new query {} at local RSP engine: " +
                            "observer already exists", centralQueryObserver, newLocalQuery.getQueryName());
                } else {
                    try {
                        localRspEngineApiManager.registerQueryObserver(
                                newLocalQuery.getQueryName(), centralQueryObserver);
                    } catch (RspEngineApiNetworkException | RspEngineApiResponseException
                             | DivideInvalidInputException e) {
                        LOGGER.warn("Could not register observer {} of local query {}",
                                centralQueryObserver, newLocalQuery.getQueryName(), e);
                    }
                }
            }
        }

        // loop over all input streams of the local RSP engine, of which the
        // corresponding input stream of the central RSP engine was used as an input
        // stream in one of the unregistered central RSP engine queries
        for (String localStreamUri : localStreamUrisOfUnregisteredCentralQueries) {
            // get translation of stream URI for central RSP engine
            // (which is the stream URI of at least 1 unregistered query)
            String centralStreamUri = getLocalToCentralStreamUriTranslation(localStreamUri);

            try {
                LOGGER.info("Processing input stream '{}' of unregistered central query: " +
                                "checking whether this stream is still used as input stream by " +
                                "another active central RSP query and disable forwarding of stream " +
                                "on local RSP engine to central RSP engine if this is not the case",
                        localStreamUri);

                // get all queries on central RSP engine that use the central stream URI
                // as an input stream
                List<JsonObject> streamQueries = this.centralRspEngineApiManager.
                        retrieveQueriesWithInputStream(centralStreamUri);

                // only continue if the list is empty: otherwise the stream forwarding
                // should remain enabled
                if (streamQueries.isEmpty()) {
                    // generate WebSocket URL of the central stream URI input WebSocket server
                    // of the central RSP engine and disable the forwarding of the
                    // original stream at the local RSP engine to this WebSocket
                    String webSocketUrl = String.format("%s/streams/%s",
                            this.centralRspEngine.getWebSocketStreamUrl(),
                            URLEncoder.encode(centralStreamUri, StandardCharsets.UTF_8.toString()));
                    this.localRspEngineApiManager.disableStreamForwardingToWebSocket(
                            localStreamUri, webSocketUrl);
                }

            } catch (UnsupportedEncodingException | RspEngineApiNetworkException
                     | RspEngineApiResponseException | DivideInvalidInputException e) {
                throw new RspEngineHandlerException(
                        String.format("The forwarding of the local stream URI %s to the WebSocket" +
                                " stream server of the central RSP engine (for central stream %s) " +
                                "could not be enabled", localStreamUri, centralStreamUri), e);
            }
        }

        // update the location for the given DIVIDE query for future query derivations
        updateLocationOfDivideQuery(divideQuery, RspLocation.LOCAL);
    }

}
