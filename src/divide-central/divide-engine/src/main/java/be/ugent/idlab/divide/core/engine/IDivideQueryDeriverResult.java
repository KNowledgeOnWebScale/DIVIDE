package be.ugent.idlab.divide.core.engine;

import java.util.List;

public interface IDivideQueryDeriverResult {

    /**
     * @return a list of substituted RSP-QL queries being the result of performing
     *         the DIVIDE query derivation and/or window parameter substitution
     */
    List<String> getSubstitutedRspQlQueries();

}
