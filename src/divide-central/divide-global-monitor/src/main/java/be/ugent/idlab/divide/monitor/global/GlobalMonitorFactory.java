package be.ugent.idlab.divide.monitor.global;

import be.ugent.idlab.divide.core.engine.IDivideEngine;
import be.ugent.idlab.divide.monitor.IDivideGlobalMonitor;

import java.util.List;

public class GlobalMonitorFactory {

    private static IDivideGlobalMonitor instance;

    public static void initialize(IDivideEngine divideEngine,
                                  List<GlobalMonitorQuery> globalMonitorQueries) throws GlobalMonitorException {
        if (instance == null) {
            instance = new GlobalMonitor(divideEngine, globalMonitorQueries);
        }
    }

    public static IDivideGlobalMonitor getInstance() {
        return instance;
    }

}
