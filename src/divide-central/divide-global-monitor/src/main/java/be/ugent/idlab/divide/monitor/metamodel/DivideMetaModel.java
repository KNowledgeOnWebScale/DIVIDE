package be.ugent.idlab.divide.monitor.metamodel;


import be.ugent.idlab.divide.core.component.IComponent;
import be.ugent.idlab.divide.core.engine.IDivideEngine;
import be.ugent.idlab.divide.core.query.IDivideQuery;
import be.ugent.idlab.divide.monitor.global.GlobalMonitorException;
import be.ugent.idlab.divide.rsp.RspLocation;
import be.ugent.idlab.divide.rsp.query.IRspQuery;
import be.ugent.idlab.reasoningservice.common.ReasoningServer;
import be.ugent.idlab.reasoningservice.common.event.Event;
import be.ugent.idlab.util.rdf.RDFLanguage;
import be.ugent.idlab.util.rdf.jena3.owlapi4.JenaUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DivideMetaModel implements IDivideMetaModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(DivideMetaModel.class.getName());

    private final DivideMetaModelMapper mapper;
    private final ReasoningServer reasoningServer;

    private static int QUERY_COUNTER = 0;

    public DivideMetaModel(ReasoningServer reasoningServer) throws GlobalMonitorException {
        this.mapper = new DivideMetaModelMapper();
        this.reasoningServer = reasoningServer;
    }

    @Override
    public void registerEngine(IDivideEngine divideEngine) {
        LOGGER.info("Registering DIVIDE engine to meta model");

        // add DIVIDE engine triples to meta model
        String triples = this.mapper.getDivideEngineTriples(divideEngine);
        updateTriplesInMetaModel(triples, true);
    }

    @Override
    public void addDivideQuery(IDivideQuery divideQuery) {
        LOGGER.info("Adding DIVIDE query {} to meta model", divideQuery.getName());

        // add DIVIDE query triples to meta model
        String triples = this.mapper.getDivideQueryTriples(divideQuery);
        updateTriplesInMetaModel(triples, true);
    }

    @Override
    public void removeDivideQuery(IDivideQuery divideQuery) {
        LOGGER.info("Removing DIVIDE query {} from meta model", divideQuery.getName());

        // remove DIVIDE query triples from meta model
        String triples = this.mapper.getDivideQueryTriples(divideQuery);
        updateTriplesInMetaModel(triples, false);

        // note: RSP queries for that DIVIDE query are removed by unregistering them
        //       from all component's RSP engines (so dedicated calls will happen for that)
        // note: DIVIDE query deployment triples are not removed
    }

    @Override
    public void updateDivideQueryDeployment(IDivideQuery divideQuery,
                                            IComponent component,
                                            RspLocation rspLocation) {
        LOGGER.info("Updating deployment of DIVIDE query {} on component {} in meta model to {}",
                divideQuery.getName(), component.getId(), rspLocation.toString());

        // remove DIVIDE query deployment triples of other RSP location
        removeDivideQueryDeployment(
                divideQuery, component,
                rspLocation == RspLocation.CENTRAL ? RspLocation.LOCAL : RspLocation.CENTRAL);

        // add DIVIDE query deployment triples to meta model
        String addedTriples = this.mapper.getDivideQueryDeploymentTriples(
                divideQuery, component, rspLocation);
        updateTriplesInMetaModel(addedTriples, true);
    }

    private void removeDivideQueryDeployment(IDivideQuery divideQuery,
                                             IComponent component,
                                             RspLocation rspLocation) {
        String removedTriples = this.mapper.getDivideQueryDeploymentTriples(
                divideQuery, component, rspLocation);
        updateTriplesInMetaModel(removedTriples, false);
    }

    @Override
    public void addComponent(IComponent component) {
        LOGGER.info("Adding DIVIDE component {} to meta model", component.getId());

        // add DIVIDE component triples to meta model
        String triples = this.mapper.getDivideComponentTriples(component);
        updateTriplesInMetaModel(triples, true);
    }

    @Override
    public void removeComponent(IComponent component) {
        LOGGER.info("Removing DIVIDE component {} from meta model", component.getId());

        // remove DIVIDE component triples from meta model
        String triples = this.mapper.getDivideComponentTriples(component);
        updateTriplesInMetaModel(triples, false);

        // note: RSP queries for that component are removed by unregistering them
        //       from the component's RSP engine (so dedicated calls will happen for that)
        // note: DIVIDE query deployment triples are not removed
    }

    @Override
    public void addRegisteredQuery(IRspQuery rspQuery) {
        LOGGER.info("Adding RSP query {} (registered to RSP engine {}, associated to component {})" +
                        " to meta model", rspQuery.getQueryName(), rspQuery.getRspEngine().getId(),
                rspQuery.getAssociatedComponent().getId());

        // add RSP query triples to meta model
        String triples = this.mapper.getRspQueryTriples(rspQuery);
        updateTriplesInMetaModel(triples, true);
    }

    @Override
    public void removeRegisteredQuery(IRspQuery rspQuery) {
        LOGGER.info("Removing RSP query {} (registered to RSP engine {}, associated to component {})" +
                        " from meta model", rspQuery.getQueryName(), rspQuery.getRspEngine().getId(),
                rspQuery.getAssociatedComponent().getId());

        // remove RSP query triples from meta model
        String triples = this.mapper.getRspQueryTriples(rspQuery);
        updateTriplesInMetaModel(triples, false);
    }

    /**
     * @param triples triples to be inserted or removed from the meta model
     * @param insert true if triples should be inserted, false if they should be deleted
     */
    synchronized void updateTriplesInMetaModel(String triples, boolean insert) {
        // translate triples to N-Triple format
        String triplesInNTriples = JenaUtilities.serializeModel(
                JenaUtilities.parseString(triples), RDFLanguage.N_TRIPLE);

        // create insert or delete event for reasoning service
        Event event;
        String queryID;
        if (insert) {
            queryID = "insert-event-" + QUERY_COUNTER++;
            event = new Event(Event.Type.INSERT_EVENT, triplesInNTriples);
        } else {
            queryID = "delete-event-" + QUERY_COUNTER++;
            event = new Event(Event.Type.DELETE_EVENT, triplesInNTriples);
        }
        event.setId(queryID);

        // add insert or delete event to event handler of reasoning server
        this.reasoningServer.getEventHandler().handleEvent(event);
    }

    @SuppressWarnings("unused")
    synchronized void executeUpdateQueryOnMetaModel(String query) {
        // create single run query for reasoning service
        String queryID = "query-" + QUERY_COUNTER++;
        Event queryEvent = new Event(Event.Type.SINGLE_RUN_QUERY, query);
        queryEvent.setId(queryID);

        // add single run query to event handler of reasoning server
        this.reasoningServer.getEventHandler().handleEvent(queryEvent);
    }

}
