package be.ugent.idlab.divide.core.engine;

import be.ugent.idlab.divide.core.context.Context;
import be.ugent.idlab.divide.core.exception.DivideInitializationException;
import be.ugent.idlab.divide.core.exception.DivideInvalidInputException;
import be.ugent.idlab.divide.core.exception.DivideNotInitializedException;
import be.ugent.idlab.divide.core.exception.DivideQueryDeriverException;
import be.ugent.idlab.divide.core.query.IDivideQuery;
import be.ugent.idlab.divide.core.query.parser.IDivideQueryParser;
import org.apache.jena.rdf.model.Model;

/**
 * Class responsible for deriving the actual RSP-QL queries from an {@link IDivideQuery}.
 */
public interface IDivideQueryDeriver {

    /**
     * Loads the ontology that needs to be used as input (TBox) for each query
     * derivation performed by this query deriver.
     *
     * If an ontology has been loaded successfully in the past at least once,
     * this method will reload the ontology based on the new input. If something
     * goes wrong during this reloading and an exception is thrown, the query
     * deriver should still be in a valid state and continue working with the
     * latest successfully loaded ontology.
     *
     * @param ontology representation of the ontology that should be used as TBox
     *                 during the DIVIDE query derivation
     * @throws DivideInvalidInputException when the ontology contains invalid statements, i.e.,
     *                                     statements which cannot be loaded by the query deriver
     * @throws DivideInitializationException when something goes wrong during the loading of the
     *                                       ontology files, which prevents the instance from
     *                                       performing the query derivation
     */
    void loadOntology(Model ontology)
            throws DivideInvalidInputException, DivideInitializationException;

    /**
     * Register a new {@link IDivideQuery} to this query deriver, to prepare
     * the engine for deriving the RSP-QL queries from this DIVIDE query.
     *
     * @param divideQuery {@link IDivideQuery} to be registered to this query deriver
     *                    (if null or if a DIVIDE query with the given name is already
     *                     registered to the engine, nothing is changed)
     * @param queryParser {@link IDivideQueryParser} to be used when the query deriver wants
     *                    to parse the context-enriching queries of the DIVIDE query to possibly
     *                    manipulate the context enrichment
     * @throws DivideQueryDeriverException when something goes wrong during the registration
     *                                     of the new DIVIDE query, which prevents the instance
     *                                     from performing the query derivation for this query
     * @throws DivideInvalidInputException when the given DIVIDE query has invalid fields
     */
    void registerQuery(IDivideQuery divideQuery,
                       IDivideQueryParser queryParser)
            throws DivideQueryDeriverException, DivideInvalidInputException;

    /**
     * Unregister a {@link IDivideQuery} from this query deriver.
     * In this way, this query deriver knows it will no longer need to derive
     * RSP-QL queries from this DIVIDE query, which means it can clean up any
     * resources related to this DIVIDE query.
     *
     * @param divideQuery {@link IDivideQuery} to be unregistered from this query deriver
     *                    (if null or if no DIVIDE query with the given name is registered
     *                     to the engine, nothing is changed)
     */
    void unregisterQuery(IDivideQuery divideQuery);

    /**
     * Performs the actual query derivation for the {@link IDivideQuery} with the given name,
     * if such a DIVIDE query is registered to this query deriver.
     * Runs the query derivation with the loaded ontology (TBox) and the passed context (ABox),
     * outputting a query deriver result containing a list of RSP-QL queries that should be
     * registered on the component with the passed ID given the new (passed) context.
     *
     * @param divideQueryName name of the {@link IDivideQuery} to be used for the query derivation
     *                        (if no DIVIDE query with this name is registered, nothing is done and
     *                         an empty list is returned)
     * @param context new context for a certain component that should be used as input for
     *                the query derivation
     * @param componentId ID of the component for which this query derivation is run
     * @return a query deriver result, containing a method to retrieve a list of RSP-QL queries
     *         derived from the given DIVIDE query (can be of any length),
     *         which should be registered on the component with the passed ID
     * @throws DivideQueryDeriverException when something goes wrong during the derivation of
     *                                     the RSP-QL queries
     * @throws DivideNotInitializedException if {@link #loadOntology(Model)} has not been called yet
     */
    IDivideQueryDeriverResult deriveQueries(String divideQueryName,
                                            Context context,
                                            String componentId)
            throws DivideQueryDeriverException, DivideNotInitializedException;

    /**
     * Substitutes new window parameters in a previous result of running the query derivation
     * via the {@link #deriveQueries(String, Context, String)} method.
     * These new window parameters can for example be imposed by a monitor component.
     * This methods does not perform the actual query derivation for the {@link IDivideQuery}
     * with the given name, but redoes the final part of the query derivation where the
     * window parameters for this query are substituted in the derived queries. The window
     * parameters that should be used are passed to this method.
     *
     * @param divideQueryName name of the {@link IDivideQuery} to be used for the query derivation
     *                        (if no DIVIDE query with this name is registered, nothing is done and
     *                         an empty list is returned)
     * @param windowParameters description of the new window parameters for the stream(s) defined in
     *                         the RSP-QL query body pattern of the given DIVIDE query (if window
     *                         parameter variables occur in the query pattern that are not redefined
     *                         by the monitor, the statically defined window parameters will be used
     *                         instead)
     * @param componentId ID of the component for which this window parameter substitution is run
     * @return a new query deriver result, containing a method to retrieve the list of updated
     *         RSP-QL queries with the new window parameters substituted into,
     *         which should be registered on the component with the passed ID
     * @throws DivideQueryDeriverException when something goes wrong during the process of generating
     *                                     the new RSP-QL queries
     * @throws DivideNotInitializedException if {@link #loadOntology(Model)} has not been called yet
     */
    IDivideQueryDeriverResult substituteWindowParameters(String divideQueryName,
                                                         Model windowParameters,
                                                         String componentId,
                                                         IDivideQueryDeriverResult lastResult)
            throws DivideQueryDeriverException, DivideNotInitializedException;

}
