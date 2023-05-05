package be.ugent.idlab.divide.monitor.global.rs;

import be.ugent.idlab.divide.core.engine.IDivideEngine;
import be.ugent.idlab.divide.monitor.global.translator.DivideTranslator;
import be.ugent.idlab.reasoningservice.common.observer.AbstractObserver;

public class GlobalMonitorReasoningServiceEngineObserver extends AbstractObserver {

    private final IDivideEngine divideEngine;

    public GlobalMonitorReasoningServiceEngineObserver(String observerID,
                                                       IDivideEngine divideEngine) {
        super(observerID);

        this.divideEngine = divideEngine;
    }

    @Override
    public void sendResults(String serialization) {
        super.sendResults(serialization);

        if (serialization != null && !serialization.trim().isEmpty()) {
            DivideTranslator.getInstance().translateMessageToDivideAction(
                    divideEngine, serialization);
        }
    }

}
