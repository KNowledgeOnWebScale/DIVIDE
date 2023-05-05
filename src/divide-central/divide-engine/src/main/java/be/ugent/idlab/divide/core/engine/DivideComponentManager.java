package be.ugent.idlab.divide.core.engine;

import be.ugent.idlab.divide.core.component.ComponentFactory;
import be.ugent.idlab.divide.core.component.IComponent;
import be.ugent.idlab.divide.core.context.Context;
import be.ugent.idlab.divide.core.exception.DivideInitializationException;
import be.ugent.idlab.divide.core.exception.DivideInvalidInputException;
import be.ugent.idlab.divide.rsp.RspQueryLanguage;
import be.ugent.idlab.divide.util.Constants;
import be.ugent.idlab.kb.IIriResolver;
import be.ugent.idlab.kb.IKnowledgeBase;
import be.ugent.idlab.kb.IKnowledgeBaseObserver;
import be.ugent.idlab.kb.exception.InvalidIriException;
import be.ugent.idlab.kb.exception.KnowledgeBaseOperationException;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class DivideComponentManager implements IKnowledgeBaseObserver<Model> {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(DivideComponentManager.class.getName());

    private final DivideEngine divideEngine;

    private final IKnowledgeBase<Model> knowledgeBase;

    private final Map<String, IComponent> registeredComponents;

    /**
     * Map which keeps track of how context IRIs are mapped to components observing
     * this context IRI, i.e., for each context IRI, it keeps track of for which
     * components the ABox with that IRI in the knowledge base partly defines its
     * context. If a change to a specific context IRI is observed, this means that
     * the query derivation should be triggered for each component in the
     * associated list.
     */
    private final Map<String, List<IComponent>> contextIriObservers;

    /**
     * Boolean representing whether RSP engine streams on a component should be paused
     * when context changes are detected that trigger the DIVIDE query derivation for
     * that component.
     */
    private final boolean pauseRspEngineStreamsOnContextChanges;

    /**
     * Creates a new instance of a {@link DivideComponentManager} associated
     * to the given {@link DivideEngine} and {@link IKnowledgeBase<Model>}.
     *
     * @param divideEngine DIVIDE engine for which the new instance should manage components,
     *                     and which will perform the query derivation if the new component
     *                     manager observes changes in the context associated to a component
     * @param knowledgeBase knowledge base that should be used to observe any changes
     *                      to the context of the managed components
     * @param pauseRspEngineStreamsOnContextChanges boolean representing whether RSP engine
     *                                              streams on a component should be paused
     *                                              when context changes are detected that
     *                                              trigger the DIVIDE query derivation for
     *                                              that component
     */
    DivideComponentManager(DivideEngine divideEngine,
                           IKnowledgeBase<Model> knowledgeBase,
                           boolean pauseRspEngineStreamsOnContextChanges) {
        this.divideEngine = divideEngine;
        this.knowledgeBase = knowledgeBase;

        this.registeredComponents = new HashMap<>();
        this.contextIriObservers = new HashMap<>();

        this.knowledgeBase.registerObserver(this);

        this.pauseRspEngineStreamsOnContextChanges = pauseRspEngineStreamsOnContextChanges;
    }

    synchronized IComponent registerComponent(String ipAddress,
                                              List<String> contextIris,
                                              RspQueryLanguage localRspQueryLanguage,
                                              int localRspEngineServerPort)
            throws DivideInvalidInputException {
        // resolve all context IRIs
        List<String> resolvedContextIris = new ArrayList<>();
        try {
            IIriResolver iriResolver = knowledgeBase.getIriResolver();
            for (String contextIri : contextIris) {
                resolvedContextIris.add(iriResolver.resolveIri(contextIri));
            }
        } catch (InvalidIriException e) {
            throw new DivideInvalidInputException("Invalid context IRI(s) which cannot be " +
                    "resolved by the DIVIDE knowledge base", e);
        }

        // create component
        IComponent component = ComponentFactory.createInstance(
                ipAddress, resolvedContextIris, localRspQueryLanguage, localRspEngineServerPort, divideEngine);

        // ensure component with that ID does not yet exist
        if (registeredComponents.containsKey(component.getId())) {
            LOGGER.warn("Trying to register component with already existing ID");
            return null;
        }

        LOGGER.info("Registering component with ID '{}'", component.getId());

        // keep track of component by ID
        registeredComponents.put(component.getId(), component);

        return component;
    }

    void addContextIriObserver(String contextIri, IComponent component) {
        if (contextIriObservers.containsKey(contextIri)) {
            contextIriObservers.get(contextIri).add(component);
        } else {
            List<IComponent> observers = new ArrayList<>();
            observers.add(component);
            contextIriObservers.put(contextIri, observers);
        }
    }

    /**
     * @return removed component if component with given ID exists and is removed
     *         from the list of registered components, null if no component with
     *         given ID exists
     */
    synchronized IComponent unregisterComponent(String componentId) {
        IComponent component = registeredComponents.remove(componentId);
        if (component != null) {
            LOGGER.info("Unregistering component with ID '{}'", componentId);

            // remove component as observer for its context IRIs
            component.getContextIris().forEach(
                    s -> contextIriObservers.get(s).remove(component));
        }

        return component;
    }

    synchronized Collection<IComponent> getRegisteredComponents() {
        return registeredComponents.values();
    }

    synchronized IComponent getRegisteredComponentById(String id) {
        return registeredComponents.get(id);
    }

    Model getContextAssociatedToComponent(String id) {
        IComponent component = registeredComponents.get(id);
        if (component != null) {
            try {
                Model componentContext = ModelFactory.createDefaultModel();
                for (String contextIri : component.getContextIris()) {
                    // get ABox associated to each context IRI
                    Model context = knowledgeBase.getABox(contextIri);

                    // add retrieved context to full context of this component
                    componentContext.add(context.listStatements());
                }
                return componentContext;

            } catch (KnowledgeBaseOperationException e) {
                // if an error occurs when retrieving the knowledge base context
                // for a given component, the context is incomplete and therefore
                // considered non-existing
                LOGGER.error(Constants.UNKNOWN_ERROR_MARKER,
                        "Error occurred when retrieving current context of" +
                                " component with ID {}",
                        component.getId(), e);

                return null;
            }

        } else {
            return null;
        }
    }

    @Override
    public synchronized void notifyABoxUpdated(String iri, Model model) {
        // check if queries need to be updated for components
        // (is the case for components observing this iri)
        boolean updateQueries = contextIriObservers.containsKey(iri) &&
                !contextIriObservers.get(iri).isEmpty();

        if (updateQueries) {
            LOGGER.info("Receiving knowledge base update for ABox with IRI '{}'", iri);

            // as soon as a context change is detected, the RSP engine should be paused
            // until further notice (i.e., until the query registration finished at some
            // point and restarts it again)
            if (pauseRspEngineStreamsOnContextChanges) {
                for (IComponent component : contextIriObservers.get(iri)) {
                    component.getRspEngineHandler().pauseRspEngineStreams();
                }
            }

            // keep track of map with fetched contexts
            Map<String, Model> contextSnapshots = new HashMap<>();

            // handle every observing component
            for (IComponent component : contextIriObservers.get(iri)) {
                try {
                    Model componentContext = ModelFactory.createDefaultModel();

                    // retrieve context for every IRI that is part of this component's context
                    for (String contextIri : component.getContextIris()) {
                        Model context;
                        if (iri.equals(contextIri)) {
                            context = model;
                        } else if (contextSnapshots.containsKey(contextIri)) {
                            context = contextSnapshots.get(contextIri);
                        } else {
                            context = knowledgeBase.getABox(contextIri);
                            contextSnapshots.put(contextIri, context);
                        }

                        // add retrieved context to full context of this component
                        componentContext.add(context.listStatements());
                    }

                    // update queries for component using its full context
                    divideEngine.enqueueGeneralDivideQueryDerivationTask(
                            component, new Context(componentContext));

                } catch (KnowledgeBaseOperationException e) {
                    // if an error occurs when retrieving the knowledge base context
                    // for a given component, no RSP query update is enqueued for this
                    // component (because the context is incomplete)
                    LOGGER.error(Constants.UNKNOWN_ERROR_MARKER,
                            "Error occurred when retrieving current context of" +
                            " component with ID {} -> queries are NOT updated",
                            component.getId(), e);
                }
            }
        }
    }

    @Override
    public synchronized void notifyTBoxUpdated(Model model) {
        Thread tBoxUpdateThread = new Thread(() -> {
            try {
                LOGGER.info("TBox of DIVIDE knowledge base updated -> reloaded as DIVIDE ontology");

                // load new ontology to the DIVIDE engine
                divideEngine.loadOntology(model);

            } catch (DivideInvalidInputException | DivideInitializationException e) {
                // if something goes wrong, it should be logged,
                // BUT the engine is guaranteed to continue working with the
                //     latest successfully loaded ontology, so no further action
                //     is required
                LOGGER.error("Reloading new TBox as DIVIDE ontology FAILED - DIVIDE engine will" +
                        " continue working with the latest successfully loaded ontology");
            }
        });
        tBoxUpdateThread.start();
    }

}
