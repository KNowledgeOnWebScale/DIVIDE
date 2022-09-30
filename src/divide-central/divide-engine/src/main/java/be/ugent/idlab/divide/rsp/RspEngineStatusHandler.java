package be.ugent.idlab.divide.rsp;

import be.ugent.idlab.divide.core.exception.DivideInvalidInputException;
import be.ugent.idlab.divide.rsp.api.IRspEngineApiManager;
import be.ugent.idlab.divide.rsp.api.RspEngineApiNetworkException;
import be.ugent.idlab.divide.rsp.api.RspEngineApiResponseException;
import be.ugent.idlab.divide.rsp.engine.IRspEngine;
import be.ugent.idlab.divide.util.LogConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.LinkedBlockingQueue;

public class RspEngineStatusHandler {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(RspEngineStatusHandler.class.getName());

    enum RspEngineStatus {
        PAUSED,
        RUNNING,
        RESTARTING_FAILED,
        UNKNOWN
    }

    private RspEngineStatus status;
    private int additionalNumberOfPauseRequests;

    private final IRspEngine rspEngine;
    private final IRspEngineApiManager rspEngineApiManager;

    private final LinkedBlockingQueue<RspEngineStatusUpdateTask> statusUpdateQueue;
    private Thread statusUpdateThread;

    private final Object guard = new Object();

    RspEngineStatusHandler(IRspEngine rspEngine,
                           IRspEngineApiManager rspEngineApiManager) {
        this.rspEngine = rspEngine;
        this.rspEngineApiManager = rspEngineApiManager;

        this.status = RspEngineStatus.RUNNING;
        this.additionalNumberOfPauseRequests = 0;

        this.statusUpdateQueue = new LinkedBlockingQueue<>();
        this.statusUpdateThread = new Thread(this::processStatusUpdates);
        this.statusUpdateThread.start();
    }

    public void pauseRspEngine() {
        enqueueStatusUpdateTask(new PauseRspEngineStreamsTask());
    }

    public void restartRspEngine() {
        enqueueStatusUpdateTask(new RestartRspEngineStreamsTask(0));
    }

    public void stopAllTasks() {
        this.statusUpdateQueue.clear();
        this.statusUpdateThread.interrupt();
        this.statusUpdateThread = null;
    }

    private void enqueueStatusUpdateTask(RspEngineStatusUpdateTask statusUpdateTask) {
        LOGGER.info("Trying to enqueue status update task of type {} at RSP engine " +
                "with base URL {}", statusUpdateTask.getClass(), rspEngine.getBaseUrl());
        synchronized (guard) {
            LOGGER.info("Status of RSP engine with base URL {} before enqueueing: {}",
                    rspEngine.getBaseUrl(), status);
            if (status == RspEngineStatus.RESTARTING_FAILED) {
                LOGGER.info("Interrupting status update thread of RSP engine with base URL {} " +
                        "which is retrying failed restart", rspEngine.getBaseUrl());
                this.statusUpdateThread.interrupt();

                // consider the status running from now on, since the restart retrial is
                // is interrupted so no confirmation of the status is received from the engine
                this.status = RspEngineStatus.UNKNOWN;
            }

            // add new status to status update queue
            LOGGER.info("ENQUEUEING status update task of type {} at RSP engine " +
                    "with base URL {}", statusUpdateTask.getClass(), rspEngine.getBaseUrl());
            this.statusUpdateQueue.add(statusUpdateTask);
        }
    }

    private void processStatusUpdates() {
        try {
            boolean interrupted = false;
            while (!interrupted) {
                // retrieve the updated context from the queue - blocks if the
                // queue is empty until an item again enters the queue
                RspEngineStatusUpdateTask statusUpdateTask = statusUpdateQueue.take();

                // update the status of the engine
                boolean interruptedDuringUpdate = executeStatusUpdateTask(statusUpdateTask);

                // check if thread has been interrupted during status update
                interrupted = interruptedDuringUpdate || Thread.currentThread().isInterrupted();
            }

            // thread is interrupted explicitly by the system, probably because
            // the component is unregistered
            LOGGER.info("Status update thread for RSP engine with base URL {} is found interrupted after" +
                            " status update, so is stopping with the processing of the status update queue",
                    rspEngine.getBaseUrl());

        } catch (InterruptedException e) {
            LOGGER.info("Status update thread for RSP engine with base URL {} is interrupted while waiting," +
                            " so is stopping with the processing of the status update queue",
                    rspEngine.getBaseUrl());
        }

        // set status update thread to null so that the engine knows a new thread
        // should be started upon arrival of a new status update request
        // (after exiting this method, the thread status will become TERMINATED)
        this.statusUpdateThread = null;

        // restart the status update thread
        restartStatusUpdateThreadIfNeeded();
    }

    private boolean executeStatusUpdateTask(RspEngineStatusUpdateTask statusUpdateTask) {
        LOGGER.info("EXECUTING status update task of type {} at RSP engine " +
                "with base URL {}", statusUpdateTask.getClass(), rspEngine.getBaseUrl());
        return statusUpdateTask.execute();
    }

    private void restartStatusUpdateThreadIfNeeded() {
        // check if thread that is processing status updates is interrupted
        // (in that case it will have been set to null),
        // and if so, create & start new thread for this
        if (this.statusUpdateThread == null) {
            LOGGER.info("Status update thread for RSP engine with base URL {} has been " +
                            "interrupted, so a new thread is started",
                    rspEngine.getBaseUrl());

            this.statusUpdateThread = new Thread(this::processStatusUpdates);
            this.statusUpdateThread.start();
        }
    }

    interface RspEngineStatusUpdateTask {
        boolean execute();
    }

    class PauseRspEngineStreamsTask implements RspEngineStatusUpdateTask {

        private final Logger LOGGER = LoggerFactory.getLogger(
                PauseRspEngineStreamsTask.class.getName());

        @Override
        public boolean execute() {
            LOGGER.info("Status of RSP engine with base URL {} before executing PAUSE task: {}",
                    rspEngine.getBaseUrl(), status);

            if (status == RspEngineStatus.PAUSED) {
                // the engine has been paused recently and has not been restarted yet
                // -> this means a new context change has arrived before the change
                //    that actually paused the engine has completed
                // -> the additional number of pause requests should be increased
                additionalNumberOfPauseRequests++;

            } else {
                // the engine is running, or restarting has been tried but has not
                // succeeded yet, or the status is unknown
                // -> in any case, it should be paused now
                pauseRspEngine();
            }

            return false;
        }

        private void pauseRspEngine() {
            try {
                // pause streams at RSP engine
                rspEngineApiManager.pauseRspEngineStreams();

                // if pausing fails, it should not be retried

            } catch (RspEngineApiNetworkException e) {
                LOGGER.error("External network error when pausing streams for RSP engine " +
                                "with base URL {}", rspEngine.getBaseUrl());

            } catch (RspEngineApiResponseException e) {
                LOGGER.error("External server error when pausing streams for RSP engine " +
                        "with base URL {}", rspEngine.getBaseUrl(), e);

            } catch (DivideInvalidInputException e) {
                // note: this will normally never occur
                LOGGER.error(LogConstants.UNKNOWN_ERROR_MARKER,
                        "Internal URL error within DIVIDE when trying to pause streams at for " +
                                "RSP engine with base URL {}", rspEngine.getBaseUrl());
            }

            synchronized (guard) {
                // both when successful and when not, the status of this RSP engine is updated
                // (if we end up here, the number of additional pause requests will always be 0)
                status = RspEngineStatus.PAUSED;
            }
        }

    }

    class RestartRspEngineStreamsTask implements RspEngineStatusUpdateTask {

        private final Logger LOGGER = LoggerFactory.getLogger(
                RestartRspEngineStreamsTask.class.getName());

        private final long sleepingTime;

        public RestartRspEngineStreamsTask(long sleepingTime) {
            this.sleepingTime = sleepingTime;
        }

        @Override
        public boolean execute() {
            LOGGER.info("Status of RSP engine with base URL {} before executing RESTART task: {}",
                    rspEngine.getBaseUrl(), status);

            if (status == RspEngineStatus.RUNNING) {
                LOGGER.info("Not restarting RSP engine with base URL {} since it is still running",
                        rspEngine.getBaseUrl());

                // if engine is already running, there is no need to restart it
                return false;
            }

            if (status == RspEngineStatus.PAUSED &&
                    additionalNumberOfPauseRequests > 0) {
                LOGGER.info("Not restarting RSP engine with base URL {} since there are still {} " +
                                "additional pause requests",
                        rspEngine.getBaseUrl(), additionalNumberOfPauseRequests);

                // if the engine is paused and there have been additional pause requests,
                // this means that this restart task is not allowed yet to actually restart
                // the engine -> but the number of additional requests can be decreased
                additionalNumberOfPauseRequests--;
                return false;
            }

            if (status == RspEngineStatus.RESTARTING_FAILED
                    && !statusUpdateQueue.isEmpty()) {
                LOGGER.info("Not restarting RSP engine with base URL {} since this is a retrial and " +
                                "there are new status update requests in the queue",
                        rspEngine.getBaseUrl());

                // if this is retrial of a failed restart, but there are other
                // tasks in the queue by now, the retrial should not even be started
                return false;
            }

            // otherwise, the engine can be restarted
            // (status is PAUSED and number of additional pause requests is 0)
            return restartRspEngine();
        }

        private boolean restartRspEngine() {
            try {
                // sleep if needed (will the case for retrial tasks)
                if (sleepingTime > 0) {
                    LOGGER.info("Sleeping for {} ms before restarting streams at RSP engine " +
                            "with base URL {}", sleepingTime, rspEngine.getBaseUrl());
                    Thread.sleep(sleepingTime);
                }

                // restart streams at RSP engine
                rspEngineApiManager.restartRspEngineStreams();

                synchronized (guard) {
                    // only if successful (i.e., if no exception is thrown),
                    // the status of this RSP engine is updated
                    status = RspEngineStatus.RUNNING;
                }

            } catch (InterruptedException e) {
                LOGGER.error("Interrupted while sleeping before retrying restarting " +
                        "RSP engine with base URL {}", rspEngine.getBaseUrl());

                // return interruption
                return true;

            } catch (RspEngineApiNetworkException e) {
                LOGGER.error("External network error when restarting streams at " +
                        "RSP engine with base URL {}", rspEngine.getBaseUrl());

                // retrying makes sense in this case
                retryRestart();

            } catch (RspEngineApiResponseException e) {
                LOGGER.error("External server error when restarting streams at " +
                        "RSP engine with base URL {}", rspEngine.getBaseUrl());

            } catch (DivideInvalidInputException e) {
                // note: this will normally never occur
                LOGGER.error(LogConstants.UNKNOWN_ERROR_MARKER,
                        "Internal URL error within DIVIDE when trying to restart streams " +
                                "RSP engine with base URL {}", rspEngine.getBaseUrl());
            }

            return false;
        }

        private void retryRestart() {
            synchronized (guard) {
                // reschedule retrial if status update queue is currently empty
                if (statusUpdateQueue.isEmpty()) {
                    // update status to RESTARTING_FAILED
                    status = RspEngineStatus.RESTARTING_FAILED;

                    long newSleepingTime;
                    if (sleepingTime == 0) {
                        newSleepingTime = 5000;
                    } else {
                        newSleepingTime = sleepingTime * 2;
                    }
                    LOGGER.info("Enqueueing retrial of restarting streams at RSP engine " +
                            "with base URL {} (sleep time {} ms)",
                            rspEngine.getBaseUrl(), newSleepingTime);
                    statusUpdateQueue.add(new RestartRspEngineStreamsTask(newSleepingTime));
                } else {
                    // update status to UNKNOWN, since no restart request is fired
                    // (so the system has no clue what the status is at this point)
                    status = RspEngineStatus.UNKNOWN;

                    LOGGER.info("Not enqueueing retrial of restarting streams at RSP engine " +
                                    "with base URL {} since there are new status update tasks",
                            rspEngine.getBaseUrl());
                }
            }
        }

    }

}
