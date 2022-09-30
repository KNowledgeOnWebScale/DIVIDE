package be.ugent.idlab.divide.api.representation.component;

import be.ugent.idlab.divide.core.component.IComponent;

import java.util.List;

@SuppressWarnings({"FieldCanBeLocal", "unused"})
public class ComponentRepresentation {

    private final String id;
    private final List<String> contextIris;
    private final RspEngineRepresentation rspEngine;

    public ComponentRepresentation(IComponent component) {
        this.id = component.getId();
        this.contextIris = component.getContextIris();
        this.rspEngine = new RspEngineRepresentation(
                component.getRspEngineHandler().getRspEngine());
    }

}
