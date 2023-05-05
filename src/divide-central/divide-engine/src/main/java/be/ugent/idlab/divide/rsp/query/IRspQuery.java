package be.ugent.idlab.divide.rsp.query;

import be.ugent.idlab.divide.core.component.IComponent;
import be.ugent.idlab.divide.core.query.IDivideQuery;
import be.ugent.idlab.divide.rsp.engine.IRspEngine;
import be.ugent.idlab.divide.rsp.query.window.IStreamWindow;

import java.util.List;

/**
 * Representation of an RSP query, which has a name and a body.
 */
public interface IRspQuery {

    /**
     * @return name of RSP query
     */
    String getQueryName();

    /**
     * @return body of RSP query
     */
    String getQueryBody();

    /**
     * @return original RSP-QL query body of RSP query
     */
    String getRspQLQueryBody();

    /**
     * @return a reference to the DIVIDE query that was instantiated into this RSP query
     */
    IDivideQuery getOriginalDivideQuery();

    void updateAfterRegistration(String id,
                                 List<IStreamWindow> inputStreams,
                                 IRspEngine rspEngine,
                                 IComponent associatedComponent);

    String getId();

    List<IStreamWindow> getStreamWindows();

    IRspEngine getRspEngine();

    int getQuerySlidingStepInSeconds();

    int getStreamWindowSizeInSeconds();

    IComponent getAssociatedComponent();

}
