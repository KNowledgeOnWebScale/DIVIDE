package be.ugent.idlab.divide.core.engine;

import be.ugent.idlab.divide.core.component.IComponent;
import be.ugent.idlab.divide.core.context.Context;
import be.ugent.idlab.divide.core.context.IContextEnricher;
import be.ugent.idlab.divide.core.query.IDivideQuery;
import be.ugent.idlab.divide.util.LogConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * Runnable which has the task of performing the derivation of a single
 * {@link IDivideQuery} on the context associated to a certain {@link IComponent}.
 *
 * This can be parallelized with doing the same task for the other DIVIDE queries
 * registered at the same component (if this task is also required for these queries).
 */
class SingleQueryDeriver implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(SingleQueryDeriver.class.getName());

    private final IDivideQuery divideQuery;
    private final Context context;
    private final IComponent component;
    private final IDivideQueryDeriver divideQueryDeriver;
    private final DivideOntology divideOntology;
    private final CountDownLatch latch;

    SingleQueryDeriver(IDivideQuery divideQuery,
                       Context context,
                       IComponent component,
                       IDivideQueryDeriver divideQueryDeriver,
                       DivideOntology divideOntology,
                       CountDownLatch latch) {
        this.divideQuery = divideQuery;
        this.context = context;
        this.component = component;
        this.divideQueryDeriver = divideQueryDeriver;
        this.divideOntology = divideOntology;
        this.latch = latch;
    }

    @Override
    public void run() {
        long start = System.currentTimeMillis();

        LOGGER.info("Running DIVIDE query derivation for query '{}' (for component with ID '{}'," +
                        "and context '{}'))",
                divideQuery.getName(), component.getId(), context.getId());

        try {
            // run context enricher updater runnable in this thread to ensure
            // that a context enricher exists for the given combination of DIVIDE
            // component and DIVIDE query, and that the given ontology is registered
            // to the context enricher
            SingleContextEnricherUpdater contextEnricherUpdater =
                    new SingleContextEnricherUpdater(
                            component, divideQuery, divideOntology);
            contextEnricherUpdater.run();

            // copy context for this DIVIDE query (to avoid overlap)
            Context copiedContext = context.copy();
            LOGGER.info("Running DIVIDE query derivation for query '{}' (for component with ID '{}'): " +
                            "copy context '{}' to new context '{}'",
                    divideQuery.getName(), component.getId(), context.getId(), copiedContext.getId());

            // then first enrich the context with the context enricher registered
            // at the given DIVIDE component for the given DIVIDE query
            IContextEnricher contextEnricher = component.getContextEnricher(divideQuery);
            contextEnricher.enrichContext(copiedContext);

            // derive all query instances for the given DIVIDE query name and up-to-date context
            // -> what about the exceptions?
            //    * DivideNotInitializedException is impossible: this is only called if an IRI
            //      for a specific component is updated, and components cannot be registered
            //      to the DIVIDE engine if it has not been initialized
            //    * DivideQueryDeriverException: is possible if issues occur in the EYE reasoning
            //      scripts; real EYE errors are unlikely since all input is valid by definition
            //      (is either query fields which are validated upon registration of a query,
            //      controlled static inputs of DIVIDE which are known to be valid, or outputs of
            //      previous reasoning steps); I/O errors can of course never be ruled out
            //    * other possible unchecked exceptions: always possible
            // -> any exception should ALWAYS be caught and ignored, since otherwise the query
            //    update processing queue of this component will block FOREVER (since it is
            //    waiting for each started thread, including this one, to count down the latch)
            //    => whatever the exception is, this thread should simply stop and count down
            //       the latch, without having scheduled any queries for registration at the
            //       RSP engine handler
            IDivideQueryDeriverResult divideQueryDeriverResult = divideQueryDeriver.deriveQueries(
                    divideQuery.getName(), copiedContext, component.getId());
            List<String> substitutedQueries = divideQueryDeriverResult.getSubstitutedRspQlQueries();

            // schedule each new query for registration
            for (String query : substitutedQueries) {
                component.getRspEngineHandler().scheduleForRegistration(query, divideQuery);
            }

            LOGGER.info("Finished DIVIDE query derivation for query '{}' in {} milliseconds" +
                            " (for component with ID '{}', and context '{}')",
                    divideQuery.getName(), System.currentTimeMillis() - start,
                    component.getId(), copiedContext.getId());

        } catch (Exception e) {
            LOGGER.error(LogConstants.UNKNOWN_ERROR_MARKER,
                    "Error during the DIVIDE query derivation for query '{}' " +
                    "(for component with ID '{}', and context '{}')",
                    divideQuery.getName(), component.getId(), context.getId(), e);

        } finally {
            // whatever happens along the way, count down latch at the end so
            // the main query derivation thread (in DivideEngine class) is not
            // blocked forever
            latch.countDown();
        }
    }

}
