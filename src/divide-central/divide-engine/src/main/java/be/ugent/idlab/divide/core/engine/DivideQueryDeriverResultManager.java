package be.ugent.idlab.divide.core.engine;

import be.ugent.idlab.divide.core.component.IComponent;
import be.ugent.idlab.divide.core.query.IDivideQuery;

import java.util.HashMap;
import java.util.Map;

class DivideQueryDeriverResultManager {

    private static final DivideQueryDeriverResultManager instance =
            new DivideQueryDeriverResultManager();

    static DivideQueryDeriverResultManager getInstance() {
        return instance;
    }

    // class code

    private final Map<String, Map<String, IDivideQueryDeriverResult>> queryDerivationResultMap;

    private DivideQueryDeriverResultManager() {
        this.queryDerivationResultMap = new HashMap<>();
    }

    synchronized void saveQueryDeriverResult(IComponent component,
                                             IDivideQuery divideQuery,
                                             IDivideQueryDeriverResult divideQueryDeriverResult) {
        // retrieve map with query derivation results for given component
        // (create one if one does not yet exist)
        if (!this.queryDerivationResultMap.containsKey(component.getId())) {
            this.queryDerivationResultMap.put(component.getId(), new HashMap<>());
        }
        Map<String, IDivideQueryDeriverResult> componentQueryDerivationResultMap =
                this.queryDerivationResultMap.get(component.getId());

        // update query derivation result for given DIVIDE query
        componentQueryDerivationResultMap.put(divideQuery.getName(), divideQueryDeriverResult);
    }

    synchronized IDivideQueryDeriverResult retrieveLatestQueryDeriverResult(IComponent component,
                                                                            IDivideQuery divideQuery) {
        Map<String, IDivideQueryDeriverResult> componentQueryDerivationResultMap =
                this.queryDerivationResultMap.get(component.getId());
        if (componentQueryDerivationResultMap == null) {
            return null;
        } else {
            return componentQueryDerivationResultMap.get(divideQuery.getName());
        }
    }

}
