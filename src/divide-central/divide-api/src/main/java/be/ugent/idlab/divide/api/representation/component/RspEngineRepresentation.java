package be.ugent.idlab.divide.api.representation.component;


import be.ugent.idlab.divide.rsp.engine.IRspEngine;
import be.ugent.idlab.divide.rsp.query.IRspQuery;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings({"FieldCanBeLocal", "unused", "WeakerAccess", "MismatchedQueryAndUpdateOfCollection"})
public class RspEngineRepresentation {

    private final String queryLanguage;
    private final String url;
    private final List<RspQueryRepresentation> registeredQueries;

    public RspEngineRepresentation(IRspEngine engine) {
        this.queryLanguage = engine.getRspQueryLanguage().toString().toLowerCase();
        this.url = engine.getBaseUrl();

        this.registeredQueries = new ArrayList<>();
        for (IRspQuery rspQuery : engine.getRegisteredQueries()) {
            registeredQueries.add(new RspQueryRepresentation(rspQuery));
        }
    }

}
