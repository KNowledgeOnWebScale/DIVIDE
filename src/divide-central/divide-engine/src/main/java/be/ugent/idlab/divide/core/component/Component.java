package be.ugent.idlab.divide.core.component;

import be.ugent.idlab.divide.core.context.IContextEnricher;
import be.ugent.idlab.divide.core.query.IDivideQuery;
import be.ugent.idlab.divide.rsp.IRspEngineHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class Component implements IComponent {

    private final String id;
    private final IRspEngineHandler rspEngineHandler;
    private final List<String> contextIris;
    private final Map<String, IContextEnricher> contextEnricherMap;

    Component(String id,
                     IRspEngineHandler rspEngineHandler,
                     List<String> contextIris) {
        this.id = id;
        this.rspEngineHandler = rspEngineHandler;
        this.contextIris = new ArrayList<>(contextIris);
        this.contextEnricherMap = new HashMap<>();
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public IRspEngineHandler getRspEngineHandler() {
        return rspEngineHandler;
    }

    @Override
    public List<String> getContextIris() {
        return contextIris;
    }

    @Override
    public synchronized void registerContextEnricher(IDivideQuery divideQuery,
                                                     IContextEnricher contextEnricher) {
        contextEnricherMap.put(divideQuery.getName(), contextEnricher);
    }

    @Override
    public synchronized void unregisterContextEnricher(IDivideQuery divideQuery) {
        contextEnricherMap.remove(divideQuery.getName());
    }

    @Override
    public synchronized IContextEnricher getContextEnricher(IDivideQuery divideQuery) {
        return contextEnricherMap.get(divideQuery.getName());
    }

}
