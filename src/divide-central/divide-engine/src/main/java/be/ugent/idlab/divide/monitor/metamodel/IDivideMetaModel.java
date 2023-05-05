package be.ugent.idlab.divide.monitor.metamodel;

import be.ugent.idlab.divide.core.component.IComponent;
import be.ugent.idlab.divide.core.engine.IDivideEngine;
import be.ugent.idlab.divide.core.query.IDivideQuery;
import be.ugent.idlab.divide.rsp.RspLocation;
import be.ugent.idlab.divide.rsp.query.IRspQuery;

public interface IDivideMetaModel {

    void registerEngine(IDivideEngine divideEngine);

    void addDivideQuery(IDivideQuery divideQuery);

    void removeDivideQuery(IDivideQuery divideQuery);

    void updateDivideQueryDeployment(IDivideQuery divideQuery,
                                     IComponent component,
                                     RspLocation rspLocation);

    void addComponent(IComponent component);

    void removeComponent(IComponent component);

    void addRegisteredQuery(IRspQuery rspQuery);

    void removeRegisteredQuery(IRspQuery rspQuery);

}
