package be.ugent.idlab.divide.monitor.metamodel;


import be.ugent.idlab.divide.core.component.IComponent;
import be.ugent.idlab.divide.core.engine.IDivideEngine;
import be.ugent.idlab.divide.core.query.IDivideQuery;
import be.ugent.idlab.divide.rsp.RspLocation;
import be.ugent.idlab.divide.rsp.query.IRspQuery;

public class DummyDivideMetaModel implements IDivideMetaModel {

    public DummyDivideMetaModel() {
        // empty on purpose
    }

    @Override
    public void registerEngine(IDivideEngine divideEngine) {
        // empty on purpose
    }

    @Override
    public void addDivideQuery(IDivideQuery divideQuery) {
        // empty on purpose
    }

    @Override
    public void removeDivideQuery(IDivideQuery divideQuery) {
        // empty on purpose
    }

    @Override
    public void updateDivideQueryDeployment(IDivideQuery divideQuery,
                                            IComponent component,
                                            RspLocation rspLocation) {
        // empty on purpose
    }

    @Override
    public void addComponent(IComponent component) {
        // empty on purpose
    }

    @Override
    public void removeComponent(IComponent component) {
        // empty on purpose
    }

    @Override
    public void addRegisteredQuery(IRspQuery rspQuery) {
        // empty on purpose
    }

    @Override
    public void removeRegisteredQuery(IRspQuery rspQuery) {
        // empty on purpose
    }

}
