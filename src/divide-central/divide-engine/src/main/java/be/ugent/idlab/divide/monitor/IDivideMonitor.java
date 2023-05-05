package be.ugent.idlab.divide.monitor;

import be.ugent.idlab.divide.core.component.IComponent;
import be.ugent.idlab.divide.core.exception.DivideNotInitializedException;
import be.ugent.idlab.divide.monitor.metamodel.IDivideMetaModel;

public interface IDivideMonitor {

    void start() throws DivideNotInitializedException, MonitorException;

    IDivideMetaModel getDivideMetaModel();

    void addComponent(IComponent component) throws MonitorException;

    void removeComponent(IComponent component) throws MonitorException;

}
