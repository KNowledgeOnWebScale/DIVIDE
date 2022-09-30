package be.ugent.idlab.divide.monitor.interfaces.rspengine;

public class RspEngineMonitorFactory {

    public static IRspEngineMonitor createRspEngineMonitor() {
        return new RspEngineMonitor();
    }

}
