package be.ugent.idlab.divide.core.engine;

import be.ugent.idlab.divide.core.component.IComponent;
import be.ugent.idlab.divide.core.context.ContextEnricherFactory;
import be.ugent.idlab.divide.core.context.IContextEnricher;
import be.ugent.idlab.divide.core.query.IDivideQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;

/**
 * Runnable to be used when the {@link IContextEnricher} associated to a given
 * DIVIDE {@link IComponent} and {@link IDivideQuery} should be updated such that
 * a {@link IContextEnricher} is created if it did not exist yet, and that the
 * given {@link DivideOntology} is registered on this context enricher.
 *
 * This can be parallelized with doing the same task for the other DIVIDE queries
 * registered at the same component (if this task is also required for these queries).
 */
class SingleContextEnricherUpdater implements Runnable {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(SingleContextEnricherUpdater.class.getName());

    private final IComponent component;
    private final IDivideQuery divideQuery;
    private final DivideOntology divideOntology;
    private final CountDownLatch latch;

    /**
     * Creates a runnable of which the {@link #run()} method should ensure that
     * a {@link IContextEnricher} is associated to the given {@link IComponent} and
     * {@link IDivideQuery}, and that this context enricher has the given
     * {@link DivideOntology} registered.
     * This means that this runnable will create a context enricher if no context
     * enricher is associated yet to the given pair of {@link IComponent} and
     * {@link IDivideQuery}, and that the given {@link DivideOntology} is registered
     * if no or another ontology is currently registered to this context enricher.
     *
     * When the task of this runnable is finished, i.e., at the end of the
     * {@link #run()} method, the given {@link CountDownLatch} should be decremented.
     *
     * @param component component of which this runnable should check the associated
     *                  context enricher
     * @param divideQuery DIVIDE query of which this runnable should check the
     *                    associated context enricher
     * @param divideOntology DIVIDE ontology that should be registered at the context
     *                       enricher associated to the given component & DIVIDE query
     * @param latch latch to be decremented when this runnable finishes its job
     */
    SingleContextEnricherUpdater(IComponent component,
                                 IDivideQuery divideQuery,
                                 DivideOntology divideOntology,
                                 CountDownLatch latch) {
        this.component = component;
        this.divideQuery = divideQuery;
        this.divideOntology = divideOntology;
        this.latch = latch;
    }

    /**
     * Creates a runnable of which the {@link #run()} method should ensure that
     * a {@link IContextEnricher} is associated to the given {@link IComponent} and
     * {@link IDivideQuery}, and that this context enricher has the given
     * {@link DivideOntology} registered.
     * This means that this runnable will create a context enricher if no context
     * enricher is associated yet to the given pair of {@link IComponent} and
     * {@link IDivideQuery}, and that the given {@link DivideOntology} is registered
     * if no or another ontology is currently registered to this context enricher.
     *
     * @param component component of which this runnable should check the associated
     *                  context enricher
     * @param divideQuery DIVIDE query of which this runnable should check the
     *                    associated context enricher
     * @param divideOntology DIVIDE ontology that should be registered at the context
     *                       enricher associated to the given component & DIVIDE query
     */
    SingleContextEnricherUpdater(IComponent component,
                                 IDivideQuery divideQuery,
                                 DivideOntology divideOntology) {
        this.component = component;
        this.divideQuery = divideQuery;
        this.divideOntology = divideOntology;
        this.latch = null;
    }

    @Override
    public void run() {
        LOGGER.info("Updating context enricher for component with ID '{}' and DIVIDE " +
                "query with name '{}', and DIVIDE ontology with ID '{}'",
                component.getId(), divideQuery.getName(), divideOntology.getId());

        // check if a context enricher is already registered for the given
        // combination of DIVIDE component and DIVIDE query
        IContextEnricher contextEnricher = component.getContextEnricher(divideQuery);

        // -> if not, action should be taken
        if (contextEnricher == null) {
            LOGGER.info("Creating context enricher for component with ID '{}' and DIVIDE " +
                            "query with name '{}' (none exists yet)",
                    component.getId(), divideQuery.getName());

            // first create a new context enricher
            contextEnricher = ContextEnricherFactory.createInstance(
                    divideQuery.getContextEnrichment(),
                    component.getId());

            // register the context enricher for the DIVIDE query to the component
            component.registerContextEnricher(divideQuery, contextEnricher);
        }

        // register the ontology triples & rules to the context enricher
        // -> if needed, an inference model can be built in parallel
        contextEnricher.registerOntology(divideOntology);

        // if a latch is specified, count it down to let the calling thread
        // know that this updating task has finished
        if (latch != null) {
            latch.countDown();
        }
    }

}
