package be.ugent.idlab.divide.core.component;

import be.ugent.idlab.divide.core.context.IContextEnricher;
import be.ugent.idlab.divide.core.query.IDivideQuery;
import be.ugent.idlab.divide.rsp.IRspEngineHandler;

import java.util.List;

/**
 * Representation of a DIVIDE component.
 * It has an ID, a list of context IRIs, and an {@link IRspEngineHandler}.
 * The list of context IRIs contains all ABox IRIs in the knowledge base that
 * represent the relevant context of this component, i.e., when updates to the ABox
 * associated to any of its context IRIs occurs, the DIVIDE query derivation of the
 * associated {@link IRspEngineHandler} should be triggered.
 */
public interface IComponent {

    /**
     * Retrieves the ID of this {@link IComponent}.
     * This ID is a unique and therefore based on the registration URL
     * of the RSP engine running on this component.
     *
     * @return the ID of this {@link IComponent}
     */
    String getId();

    String getIpAddress();

    /**
     * Retrieves the different context IRIs of this {@link IComponent}. This is a list
     * of all ABox IRIs in the knowledge base that represent the relevant context of
     * this component, i.e., when updates to the ABox associated to any of these context
     * IRIs occurs, the DIVIDE query derivation of the associated {@link IRspEngineHandler}
     * should be triggered.
     *
     * @return the different context IRIs of this {@link IComponent}
     */
    List<String> getContextIris();

    /**
     * Retrieves the {@link IRspEngineHandler} of this component that manages the
     * RSP engine running on this component. In concrete, it handles the queries
     * registered to this engine, to ensure that the relevant queries are being
     * executed by this RSP engine at all times.
     *
     * @return the {@link IRspEngineHandler} of this component that manages the
     *         RSP engine running on this component
     */
    IRspEngineHandler getRspEngineHandler();

    void registerContextEnricher(IDivideQuery divideQuery, IContextEnricher contextEnricher);

    void unregisterContextEnricher(IDivideQuery divideQuery);

    IContextEnricher getContextEnricher(IDivideQuery divideQuery);

}
