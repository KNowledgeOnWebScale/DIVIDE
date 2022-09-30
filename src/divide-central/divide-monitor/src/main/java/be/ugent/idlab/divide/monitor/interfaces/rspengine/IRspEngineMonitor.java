package be.ugent.idlab.divide.monitor.interfaces.rspengine;

/**
 * Monitor of an individual RSP engine, offering callback methods in its interface
 * via which an RSP engine can publish any monitoring information.
 */
public interface IRspEngineMonitor {

    /**
     * Report the end of execution of a given query, containing different metrics
     * about the ended query execution.
     *
     * @param queryId ID of the query running on the RSP engine of which the execution has ended
     * @param executionTimeMs execution time of the query in milliseconds
     * @param memoryUsageMb memory usage of the query in megabytes
     */
    void finishQueryExecution(String queryId,
                              long executionTimeMs,
                              double memoryUsageMb);

}
