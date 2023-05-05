package be.ugent.idlab.divide.core.engine;

import be.ugent.idlab.divide.core.component.IComponent;
import be.ugent.idlab.divide.core.query.IDivideQuery;
import be.ugent.idlab.divide.util.Constants;
import be.ugent.idlab.util.io.IOUtilities;
import be.ugent.idlab.util.rdf.RDFLanguage;
import be.ugent.idlab.util.rdf.jena3.owlapi4.JenaUtilities;
import org.apache.jena.rdf.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * Runnable which has the task of updating the window parameters of the latest derived
 * queries of a given {@link IDivideQuery} for a given {@link IComponent}.
 *
 * This can be parallelized with doing the same task for the other DIVIDE queries
 * registered at the same component (if this task is also required for these queries).
 */
class SingleQueryWindowParameterUpdater implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(
            SingleQueryWindowParameterUpdater.class.getName());

    private final IDivideQuery divideQuery;
    private final IComponent component;
    private final Model windowParameters;
    private final IDivideQueryDeriver divideQueryDeriver;
    private final CountDownLatch latch;

    SingleQueryWindowParameterUpdater(IDivideQuery divideQuery,
                                      IComponent component,
                                      Model windowParameters,
                                      IDivideQueryDeriver divideQueryDeriver,
                                      CountDownLatch latch) {
        this.divideQuery = divideQuery;
        this.component = component;
        this.windowParameters = windowParameters;
        this.divideQueryDeriver = divideQueryDeriver;
        this.latch = latch;
    }

    @Override
    public void run() {
        long start = System.currentTimeMillis();

        LOGGER.info("Running DIVIDE query window parameter update for query '{}' " +
                        "(for component with ID '{}', and the following window parameters: '{}')",
                divideQuery.getName(), component.getId(), IOUtilities.removeWhiteSpace(
                        JenaUtilities.serializeModel(windowParameters, RDFLanguage.N_TRIPLE)));

        try {
            // ontology should already be initialized

            // retrieve latest query derivation result for the given component and DIVIDE query
            IDivideQueryDeriverResult latestQueryDeriverResult =
                    DivideQueryDeriverResultManager.getInstance().
                            retrieveLatestQueryDeriverResult(component, divideQuery);

            // do not proceed if latest result does not exist or has no substituted queries
            // associated to it: this means no update can be made or makes sense
            if (latestQueryDeriverResult != null &&
                    latestQueryDeriverResult.getSubstitutedRspQlQueries() != null &&
                    !latestQueryDeriverResult.getSubstitutedRspQlQueries().isEmpty()) {

                // substitute the updated window parameters into the latest query deriver result
                // for the given component and DIVIDE query
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
                IDivideQueryDeriverResult divideQueryDeriverResult =
                        divideQueryDeriver.substituteWindowParameters(
                                divideQuery.getName(), windowParameters,
                                component.getId(), latestQueryDeriverResult);
                List<String> substitutedQueries = divideQueryDeriverResult.getSubstitutedRspQlQueries();

                // schedule each new query for registration
                for (String query : substitutedQueries) {
                    component.getRspEngineHandler().scheduleForRegistration(query, divideQuery);
                }

                LOGGER.info("Finished DIVIDE query window parameter update for query '{}' " +
                                "in {} milliseconds (for component with ID '{}')",
                        divideQuery.getName(), System.currentTimeMillis() - start, component.getId());

            } else {
                LOGGER.warn("Not performing DIVIDE query window parameter update for query '{}' " +
                                "(for component with ID '{}') since no queries are currently registered " +
                                "(i.e., derived in the previous derivation) for the given combination " +
                                "of DIVIDE query and component",
                        divideQuery.getName(), component.getId());
            }

        } catch (Exception e) {
            LOGGER.error(Constants.UNKNOWN_ERROR_MARKER,
                    "Error during the DIVIDE query window parameter update for query '{}' " +
                    "(for component with ID '{}')",
                    divideQuery.getName(), component.getId(), e);

        } finally {
            // whatever happens along the way, count down latch at the end so
            // the main query derivation thread (in DivideEngine class) is not
            // blocked forever
            latch.countDown();
        }
    }

}
