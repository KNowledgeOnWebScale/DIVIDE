package be.ugent.idlab.divide.queryderivation.eye;

import be.ugent.idlab.divide.core.engine.IDivideQueryDeriver;
import be.ugent.idlab.divide.core.exception.DivideQueryDeriverException;

public class EyeDivideQueryDeriverFactory {

    /**
     * Returns a new instance of {@link IDivideQueryDeriver} which uses
     * the EYE reasoner to perform the query derivation.
     *
     * @param handleTBoxDefinitionsInContext boolean specifying whether the EYE query deriver
     *                                       should allow to specify TBox definitions in the
     *                                       context updates sent for the query derivation;
     *                                       if true, this means that the EYE query deriver should
     *                                       scan the context for new OWL-RL axioms and rules
     *                                       upon each query derivation call, heavily impacting
     *                                       the duration of the query derivation task (since EYE
     *                                       will create a new image, starting from the preloaded
     *                                       ontology image, with all new rules appended that follow
     *                                       from the processing of any TBox definitions in the context)
     * @return a new instance of {@link IDivideQueryDeriver} based on the EYE reasoner
     * @throws DivideQueryDeriverException when something goes wrong during the initialization
     *                                     of the new query deriver
     */
    public static IDivideQueryDeriver createInstance(boolean handleTBoxDefinitionsInContext)
            throws DivideQueryDeriverException {
        return new EyeDivideQueryDeriver(handleTBoxDefinitionsInContext);
    }

}
