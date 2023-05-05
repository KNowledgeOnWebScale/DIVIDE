package be.ugent.idlab.divide.monitor.metamodel;

import be.ugent.idlab.divide.monitor.global.GlobalMonitorException;
import be.ugent.idlab.reasoningservice.common.ReasoningServer;

public class DivideMetaModelFactory {

    public static IDivideMetaModel createInstance(ReasoningServer reasoningServer)
            throws GlobalMonitorException {
        return new DivideMetaModel(reasoningServer);
    }

}
