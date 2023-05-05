package be.ugent.idlab.divide.monitor;

import be.ugent.idlab.divide.monitor.metamodel.IDivideMetaModel;

public interface IDivideGlobalMonitor {

    void start();

    IDivideMetaModel getDivideMetaModel();

}
