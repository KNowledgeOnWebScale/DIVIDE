package be.ugent.idlab.divide.monitor;

import be.ugent.idlab.divide.core.engine.IDivideEngine;
import be.ugent.idlab.divide.core.exception.DivideInvalidInputException;

public class DivideMonitorFactory {

    /**
     * Creates an {@link IDivideMonitor} object that will handle the given DIVIDE Global Monitor
     * and manage the remote deployment of the DIVIDE Local Monitor via the given JAR.
     *
     * @param divideGlobalMonitor instance of the DIVIDE Global Monitor
     * @param divideLocalMonitorJarPath path to the JAR file of the DIVIDE Local Monitor which can be
     *                                  used to remotely deploy to all known DIVIDE components
     * @return a new instance of {@link IDivideMonitor} that represents the DIVIDE monitor
     * @throws DivideInvalidInputException when the list of task queries is not a non-empty list
     *                                     of valid SPARQL queries
     */
    public static IDivideMonitor createInstance(IDivideEngine divideEngine,
                                                IDivideGlobalMonitor divideGlobalMonitor,
                                                String divideLocalMonitorJarPath,
                                                String deviceNetworkIp)
            throws DivideInvalidInputException {
        return new DivideMonitor(divideEngine, divideGlobalMonitor,
                divideLocalMonitorJarPath, deviceNetworkIp);
    }

}
