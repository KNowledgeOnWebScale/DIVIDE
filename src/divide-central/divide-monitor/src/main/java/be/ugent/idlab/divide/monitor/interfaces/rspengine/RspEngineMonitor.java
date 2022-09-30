package be.ugent.idlab.divide.monitor.interfaces.rspengine;

class RspEngineMonitor implements IRspEngineMonitor {

    @Override
    public void finishQueryExecution(String queryId, long executionTimeMs, double memoryUsageMb) {
        // TODO MONITOR: 06/08/2020 implement
        // send execution time, memory usage & corresponding query ID to monitor stream
    }

}
