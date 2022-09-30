package be.ugent.idlab.divide.queryderivation;

import be.ugent.idlab.divide.core.engine.IDivideQueryDeriver;
import be.ugent.idlab.divide.core.exception.DivideQueryDeriverException;
import be.ugent.idlab.divide.queryderivation.eye.EyeDivideQueryDeriverFactory;

public class DivideQueryDeriverFactory {

    /**
     * Create an instance of an {@link IDivideQueryDeriver} based on the given
     * {@link DivideQueryDeriverType}.
     *
     * @param type {@link DivideQueryDeriverType} of the created {@link IDivideQueryDeriver},
     *             i.e., method or reasoner used to perform the query derivation
     * @param handleTBoxDefinitionsInContext boolean specifying whether the query deriver
     *                                       should allow to specify TBox definitions in the
     *                                       context updates sent for the query derivation;
     *                                       if true, this means that the query deriver should
     *                                       scan the context for new OWL-RL axioms and rules
     *                                       upon each query derivation call, heavily impacting
     *                                       the duration of the query derivation task
     * @return a new instance of {@link IDivideQueryDeriver} that is of the given type,
     *         i.e., that used the corresponding method or reasoner to perform it query derivation
     * @throws DivideQueryDeriverException when something goes wrong during the initialization of the
     *                                     newly created {@link IDivideQueryDeriver}
     * @throws IllegalArgumentException if no valid {@link DivideQueryDeriverType} is given
     *                                  (i.e., when it is null)
     */
    @SuppressWarnings("SwitchStatementWithTooFewBranches")
    public static IDivideQueryDeriver createInstance(DivideQueryDeriverType type,
                                                     boolean handleTBoxDefinitionsInContext)
            throws DivideQueryDeriverException {
        if (type == null) {
            throw new IllegalArgumentException("No valid query deriver type given");
        }
        switch (type) {
            case EYE:
                return EyeDivideQueryDeriverFactory.createInstance(handleTBoxDefinitionsInContext);

            default:
                throw new IllegalArgumentException("No valid query deriver type given");
        }
    }

}
