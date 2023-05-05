package be.ugent.idlab.divide.monitor.local;

public interface ILocalMonitorService {

    void start() throws LocalMonitorException;

    void reset();

}
