package be.ugent.idlab.divide.api.representation.component;

import be.ugent.idlab.divide.core.component.IComponent;

import java.util.List;

@SuppressWarnings({"FieldCanBeLocal", "unused"})
public class ComponentRepresentation {

    private final String ipAddress;
    private final String id;
    private final List<String> contextIris;
    private final RspEngineRepresentation localRspEngine;
    private final RspEngineRepresentation centralRspEngine;

    public ComponentRepresentation(IComponent component) {
        this.ipAddress = component.getIpAddress();
        this.id = component.getId();
        this.contextIris = component.getContextIris();
        this.localRspEngine = new RspEngineRepresentation(
                component.getRspEngineHandler().getLocalRspEngine());
        if (component.getRspEngineHandler().getCentralRspEngine() != null) {
            this.centralRspEngine = new RspEngineRepresentation(
                    component.getRspEngineHandler().getCentralRspEngine());
        } else {
            this.centralRspEngine = null;
        }
    }

}
