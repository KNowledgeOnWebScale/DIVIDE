package be.ugent.idlab.divide.rsp;

import be.ugent.idlab.divide.core.exception.DivideInvalidInputException;
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
import be.ugent.idlab.divide.rsp.translate.IQueryTranslator;
import be.ugent.idlab.divide.rsp.translate.QueryTranslatorFactory;
import be.ugent.idlab.divide.util.LogConstants;
import be.ugent.idlab.util.io.IOUtilities;
import org.apache.jena.atlas.lib.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

class RspEngineHandler implements IRspEngineHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(RspEngineHandler.class.getName());

    private final IRspEngine rspEngine;
    private final List<IRspQuery> scheduledQueries;

    private final IRspEngineApiManager rspEngineApiManager;
    private final IQueryTranslator queryTranslator;

    private long queryCounter;

    private final String id;

    private final RspEngineStatusHandler rspEngineStatusHandler;

    /**
     * Scheduled executor used to retry query registrations that failed because
     * of a network error
     */
    private ScheduledThreadPoolExecutor retrialScheduledExecutor;

    private Future<?> retrialFuture;
    private final Boolean retrialFutureGuard = false;

    RspEngineHandler(RspQueryLanguage rspQueryLanguage,
                     String url) throws DivideInvalidInputException {
        // make sure trailing '/' is removed from registration url
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }

        this.rspEngine = RspEngineFactory.createInstance(rspQueryLanguage, url);
        this.scheduledQueries = new ArrayList<>();

        this.rspEngineApiManager = RspEngineApiManagerFactory.createInstance(this.rspEngine);
        this.queryTranslator = QueryTranslatorFactory.createInstance(rspQueryLanguage);

        this.queryCounter = 0;

        this.id = generateAlphabeticId();

        // create retrial executor
        this.retrialScheduledExecutor = (ScheduledThreadPoolExecutor)
                Executors.newScheduledThreadPool(1);
        this.retrialScheduledExecutor.setRemoveOnCancelPolicy(true);

        this.rspEngineStatusHandler = new RspEngineStatusHandler(rspEngine, rspEngineApiManager);
    }

    @Override
    public IRspEngine getRspEngine() {
        return rspEngine;
    }

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
        String queryName = String.format("Q%d%s", queryCounter++, id);

        LOGGER.info("Scheduling RSP-QL query with name '{}' for registration at {}: {}",
                queryName,
                rspEngine.getRegistrationUrl(),
                IOUtilities.removeWhiteSpace(rspQLQueryBody));

        // translate query according to RSP query language
        String translatedQueryBody = queryTranslator.translateQuery(
                rspQLQueryBody, queryName);

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
        scheduledQueries.add(query);

        LOGGER.info("Query '{}' translated and scheduled for registration at {} with body: {}",
                queryName,
                rspEngine.getRegistrationUrl(),
                query.getQueryBody());
    }

    @Override
    public synchronized void updateRegistration(IDivideQuery divideQuery) {
        LOGGER.info("Updating RSP engine queries associated to DIVIDE query '{}' at {}",
                divideQuery.getName(), rspEngine.getRegistrationUrl());

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
        List<Pair<IRspQuery, Boolean>> queriesToRetry =
                updateRegistration(previousQueries, scheduledQueries);

        // remove all processed schedules queries from list
        this.scheduledQueries.removeAll(scheduledQueries);

        // retry queries for which the (un)registering failed due to network issue
        // (but of course only if there are queries to be retried)
        if (!queriesToRetry.isEmpty()) {
            synchronized (this.retrialFutureGuard) {
                LOGGER.info("Query update at {} for DIVIDE query '{}': " +
                                "rescheduling retrial of {} failed queries",
                        rspEngine.getRegistrationUrl(),
                        divideQuery.getName(),
                        queriesToRetry.size());
                this.retrialFuture = retrialScheduledExecutor.schedule(
                        new QueryRegistrationUpdateRetrialTask(queriesToRetry, 10),
                        10, TimeUnit.SECONDS);
            }
        } else {
            LOGGER.info("Finished query update for DIVIDE query '{}' at {} - no queries to retry",
                    divideQuery.getName(), rspEngine.getRegistrationUrl());
        }
    }

    @Override
    public synchronized void updateRegistration() {
        LOGGER.info("Updating RSP engine queries at {}",
                rspEngine.getRegistrationUrl());

        // obtain currently registered queries in RSP engine
        // and create copy of scheduled queries
        List<IRspQuery> previousQueries = new ArrayList<>(rspEngine.getRegisteredQueries());
        List<IRspQuery> scheduledQueries = new ArrayList<>(this.scheduledQueries);

        // perform registration update
        List<Pair<IRspQuery, Boolean>> queriesToRetry =
                updateRegistration(previousQueries, scheduledQueries);

        // whatever happens, all queries should be removed from the list
        // of scheduled queries
        // -> if registration succeeded, the reasons are obvious
        // -> if registration failed, it will only be retried the next time this
        //    method is called, i.e., at the next context update; if the query still
        //    needs to be registered at that point, it will again be the output of
        //    the query derivation and will therefore have been added again to the
        //    list of scheduled queries
        this.scheduledQueries.clear();

        // retry queries for which the (un)registering failed due to network issue
        // (but of course only if there are queries to be retried)
        if (!queriesToRetry.isEmpty()) {
            synchronized (this.retrialFutureGuard) {
                LOGGER.info("Query update at {}: rescheduling retrial of {} failed queries",
                        rspEngine.getRegistrationUrl(), queriesToRetry.size());
                this.retrialFuture = retrialScheduledExecutor.schedule(
                        new QueryRegistrationUpdateRetrialTask(queriesToRetry, 10),
                        10, TimeUnit.SECONDS);
            }
        } else {
            LOGGER.info("Finished query update at {} - no queries to retry",
                    rspEngine.getRegistrationUrl());
        }
    }

    private synchronized List<Pair<IRspQuery, Boolean>> updateRegistration(
            List<IRspQuery> previousQueries, List<IRspQuery> scheduledQueries) {
        // stop all query retrials
        stopQueryUpdateRetrials();

        // create empty list of queries that should be retried
        List<Pair<IRspQuery, Boolean>> queriesToRetry = new ArrayList<>();

        LOGGER.info("Query update at {}: scheduled query names: {} - " +
                        "currently registered query names: {}",
                rspEngine.getRegistrationUrl(),
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
            //  except removing it from the scheduled list, which has been done in the call above)
            if (!scheduledAgain) {
                try {
                    // unregister query from RSP engine
                    rspEngineApiManager.unregisterQuery(previousQuery);

                    // only if successful (i.e., if no exception is thrown),
                    // the blueprint of this RSP engine's queries is also updated
                    rspEngine.removeRegisteredQuery(previousQuery);

                    // if a failure occurs when unregistering this query, the RSP engine's
                    // blueprint of queries is not updated (i.e., this query is not removed
                    // from the list)
                    // => at the following call of this method, this blueprint tells DIVIDE
                    //    that this query is still registered on the engine, and that it
                    //    should again be tried to unregister this query (unless by then it
                    //    is again part of the scheduled queries)

                } catch (RspEngineApiNetworkException e) {
                    LOGGER.error("External network error when unregistering query '{}' at {}",
                            previousQuery.getQueryName(), rspEngine.getRegistrationUrl());

                    // retrying the request could potentially solve the issue since this is
                    // a network error (i.e., the destination could not be reached)
                    // -> most likely there are network connection issues
                    //    OR the RSP engine server is down
                    queriesToRetry.add(Pair.create(previousQuery, false));

                    // TODO MONITOR: 28/01/2021 do something with fact that RSP engine server might be down?

                } catch (RspEngineApiResponseException e) {
                    LOGGER.error("External server error when unregistering query '{}' at {}",
                            previousQuery.getQueryName(), rspEngine.getRegistrationUrl(), e);

                    // retrying the request is NOT useful, since this is an RSP engine server error
                    // (and the RSP engine server should ensure it can handle the registration
                    //  requests sent by DIVIDE)

                    // TODO MONITOR: 28/01/2021 do something with fact that RSP engine server cannot
                    //  properly handle registration request?

                } catch (DivideInvalidInputException e) {
                    // note: this will normally never occur
                    LOGGER.error(LogConstants.UNKNOWN_ERROR_MARKER,
                            "Internal URL error within DIVIDE when trying to unregister " +
                                    "query '{}' at {}",
                            previousQuery.getQueryName(), rspEngine.getRegistrationUrl());

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
                rspEngineApiManager.registerQuery(query);

                // only if successful (i.e., if no exception is thrown),
                // the blueprint of this RSP engine's queries is also updated
                rspEngine.addRegisteredQuery(query);

                // if a failure occurs when registering this query, the RSP engine's
                // blueprint of queries is not updated (i.e., this query is not removed
                // from the list)
                // => at the following call of this method, this blueprint tells DIVIDE
                //    that this query is not registered yet on the engine; if it is again
                //    part of the scheduled queries, it should then still be registered
                //    (instead of ignoring it since it is already considered registered)

            } catch (RspEngineApiNetworkException e) {
                LOGGER.error("External network error when registering query '{}' at {}",
                        query.getQueryName(), rspEngine.getRegistrationUrl());

                // retrying the request could potentially solve the issue since this is
                // a network error (i.e., the destination could not be reached)
                // -> most likely there are network connection issues
                //    OR the RSP engine server is down
                queriesToRetry.add(Pair.create(query, true));

                // TODO MONITOR: 28/01/2021 do something with fact that RSP engine server might be down?

            } catch (RspEngineApiResponseException e) {
                LOGGER.error("External server error when registering query '{}' at {}",
                        query.getQueryName(), rspEngine.getRegistrationUrl(), e);

                // retrying the request is NOT useful, since this is an RSP engine server error
                // (and the RSP engine server should ensure it can handle the registration
                //  requests sent by DIVIDE)

                // TODO MONITOR: 28/01/2021 do something with fact that RSP engine server cannot
                //  properly handle registration request?

            } catch (RspEngineApiInputException e) {
                // note: DivideInvalidInputException will normally never occur
                LOGGER.error(LogConstants.UNKNOWN_ERROR_MARKER,
                        "Internal query error within DIVIDE when trying to register query '{}' at {}",
                        query.getQueryName(), rspEngine.getRegistrationUrl());

                // retrying the request is NOT useful, since this error represents an
                // internal condition that will not change

            } catch (DivideInvalidInputException e) {
                // note: this will normally never occur
                LOGGER.error(LogConstants.UNKNOWN_ERROR_MARKER,
                        "Internal URL error within DIVIDE when trying to register query '{}' at {}",
                        query.getQueryName(), rspEngine.getRegistrationUrl());

                // retrying the request is NOT useful, since this error represents an
                // internal condition that will not change
            }
        }

        return queriesToRetry;
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
                LOGGER.error(LogConstants.UNKNOWN_ERROR_MARKER,
                        "Awaiting termination not finished after 1 minute => hard shutdown");
            }
        } catch (InterruptedException e) {
            LOGGER.error(LogConstants.UNKNOWN_ERROR_MARKER,
                    "Interruption while awaiting termination of retrial tasks => hard shutdown");
            retrialScheduledExecutor.shutdownNow();
        }

        // create a new retrial executor
        this.retrialScheduledExecutor = (ScheduledThreadPoolExecutor)
                Executors.newScheduledThreadPool(1);
        this.retrialScheduledExecutor.setRemoveOnCancelPolicy(true);
    }

    private String preprocessQueryBeforeRegistration(String query) {
        // preprocess query before registration by removing all unnecessary whitespace
        return IOUtilities.removeWhiteSpace(query).trim();
    }

    private String generateAlphabeticId() {
        int leftLimit = 97; // letter 'a'
        int rightLimit = 122; // letter 'z'
        int targetStringLength = 5;
        Random random = new Random();

        return random.ints(leftLimit, rightLimit + 1)
                .limit(targetStringLength)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }

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

        public QueryRegistrationUpdateRetrialTask(List<Pair<IRspQuery, Boolean>> queryPairs,
                                                  long delayBeforeRetrial) {
            this.queryPairs = queryPairs;
            this.delayBeforeRetrial = delayBeforeRetrial;
        }

        @Override
        public void run() {
            LOGGER.info("Starting retrial of updating registration at {} for {} queries: {}",
                    rspEngine.getRegistrationUrl(),
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
                        rspEngine.getRegistrationUrl(),
                        register ? "registering" : "unregistering",
                        query.getQueryName());

                try {
                    // register or unregister query at RSP engine
                    if (register) {
                        rspEngineApiManager.registerQuery(query);
                    } else {
                        rspEngineApiManager.unregisterQuery(query);
                    }

                    // only if successful (i.e., if no exception is thrown),
                    // the blueprint of this RSP engine's queries is also updated
                    rspEngine.addRegisteredQuery(query);

                } catch (RspEngineApiNetworkException e) {
                    LOGGER.error("External network error when registering query '{}' at {}",
                            query.getQueryName(), rspEngine.getRegistrationUrl());

                    // retry once again if registration failed again
                    queriesToRetry.add(Pair.create(query, register));

                } catch (RspEngineApiResponseException e) {
                    LOGGER.error("External server error when registering query '{}' at {}",
                            query.getQueryName(), rspEngine.getRegistrationUrl(), e);

                    // retrying the request is NOT useful, since this is an RSP engine server error
                    // (and the RSP engine server should ensure it can handle the registration
                    //  requests sent by DIVIDE)

                    // TODO MONITOR: 28/01/2021 do something with fact that RSP engine server cannot
                    //  properly handle registration request?

                } catch (RspEngineApiInputException e) {
                    // note: DivideInvalidInputException will normally never occur
                    LOGGER.error(LogConstants.UNKNOWN_ERROR_MARKER,
                            "Internal query error within DIVIDE when trying to register query '{}' at {}",
                            query.getQueryName(), rspEngine.getRegistrationUrl());

                    // retrying the request is NOT useful, since this error represents an
                    // internal condition that will not change

                } catch (DivideInvalidInputException e) {
                    // note: this will normally never occur
                    LOGGER.error(LogConstants.UNKNOWN_ERROR_MARKER,
                            "Internal URL error within DIVIDE when trying to register query '{}' at {}",
                            query.getQueryName(), rspEngine.getRegistrationUrl());

                    // retrying the request is NOT useful, since this error represents an
                    // internal condition that will not change
                }

                // if the thread has been interrupted, then stop going over all queries and
                // immediately return (also do not schedule a new retrial task, obviously)
                if (Thread.currentThread().isInterrupted()) {
                    LOGGER.info("Query update retrial at {}: task interrupted after updating {}",
                            rspEngine.getRegistrationUrl(),
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
                                rspEngine.getRegistrationUrl(), queriesToRetry.size());

                        // schedule new retry, with a doubled delay
                        retrialFuture = retrialScheduledExecutor.schedule(
                                new QueryRegistrationUpdateRetrialTask(
                                        queriesToRetry, delayBeforeRetrial * 2),
                                delayBeforeRetrial * 2,
                                TimeUnit.SECONDS);

                    } else {
                        LOGGER.info("Query update retrial at {}: NOT rescheduling retrial " +
                                        "of {} failed queries because of interruption",
                                rspEngine.getRegistrationUrl(), queriesToRetry.size());
                    }
                }
            } else {
                LOGGER.info("Finished query update retrial at {} - no queries to retry",
                        rspEngine.getRegistrationUrl());
            }
        }

    }

    @Override
    public void unregisterAllQueries() {
        LOGGER.info("Unregistering all RSP engine queries at {}",
                rspEngine.getRegistrationUrl());

        // stop all query update retrials since the associated component will be unregistered
        stopQueryUpdateRetrials();

        // create new list of all registered queries
        List<IRspQuery> queriesToUnregister =
                new ArrayList<>(rspEngine.getRegisteredQueries());

        if (!queriesToUnregister.isEmpty()) {
            LOGGER.info("Unregistering the following queries at {}: {}",
                    rspEngine.getRegistrationUrl(),
                    Arrays.toString(queriesToUnregister.stream().map(
                            IRspQuery::getQueryName).toArray()));

            // unregister queries
            for (IRspQuery query : queriesToUnregister) {
                unregisterQuery(query);
            }

        } else {
            LOGGER.info("No RSP engine queries registered anymore at {}",
                    rspEngine.getRegistrationUrl());
        }
    }

    @Override
    public void unregisterAllQueriesOriginatingFromDivideQuery(IDivideQuery divideQuery) {
        LOGGER.info("Unregistering RSP engine queries of DIVIDE query '{}' at {}",
                divideQuery.getName(), rspEngine.getRegistrationUrl());

        // stop all query update retrials since otherwise some new queries associated
        // to this removed DIVIDE query might be re-registered
        stopQueryUpdateRetrials();

        // retrieve list of all queries associated to the given DIVIDE query
        List<IRspQuery> queriesToUnregister = rspEngine.getRegisteredQueries()
                .stream()
                .filter(rspQuery -> divideQuery.equals(rspQuery.getOriginalDivideQuery()))
                .collect(Collectors.toList());

        if (!queriesToUnregister.isEmpty()) {
            LOGGER.info("Unregistering the following queries at {}: {}",
                    rspEngine.getRegistrationUrl(),
                    Arrays.toString(queriesToUnregister.stream().map(
                            IRspQuery::getQueryName).toArray()));

            // unregister queries
            for (IRspQuery query : queriesToUnregister) {
                unregisterQuery(query);
            }

        } else {
            LOGGER.info("No RSP engine queries registered at {} that are associated to" +
                            " DIVIDE query '{}'",
                    rspEngine.getRegistrationUrl(), divideQuery.getName());
        }
    }

    private void unregisterQuery(IRspQuery query) {
        try {
            // unregister query from RSP engine
            rspEngineApiManager.unregisterQuery(query);

            // only if successful (i.e., if no exception is thrown),
            // the blueprint of this RSP engine's queries is also updated
            rspEngine.removeRegisteredQuery(query);

            // if unregistering fails, it is what it is and it should not be retried

        } catch (RspEngineApiNetworkException e) {
            LOGGER.error("External network error when unregistering query '{}' at {}",
                    query.getQueryName(), rspEngine.getRegistrationUrl());

        } catch (RspEngineApiResponseException e) {
            LOGGER.error("External server error when unregistering query '{}' at {}",
                    query.getQueryName(), rspEngine.getRegistrationUrl(), e);

        } catch (DivideInvalidInputException e) {
            // note: this will normally never occur
            LOGGER.error(LogConstants.UNKNOWN_ERROR_MARKER,
                    "Internal URL error within DIVIDE when trying to unregister " +
                            "query '{}' at {}",
                    query.getQueryName(), rspEngine.getRegistrationUrl());
        }
    }

    @Override
    public synchronized void pauseRspEngineStreams() {
        LOGGER.info("Pausing streams of RSP engine with base URL {}", rspEngine.getBaseUrl());
        rspEngineStatusHandler.pauseRspEngine();
    }

    @Override
    public synchronized void restartRspEngineStreams() {
        LOGGER.info("Restarting streams of RSP engine with base URL {}", rspEngine.getBaseUrl());
        rspEngineStatusHandler.restartRspEngine();
    }

    @Override
    public void stopRspEngineStreamsUpdates() {
        LOGGER.info("Stopping streams updates for RSP engine with base URL {}", rspEngine.getBaseUrl());
        rspEngineStatusHandler.stopAllTasks();
    }

}
