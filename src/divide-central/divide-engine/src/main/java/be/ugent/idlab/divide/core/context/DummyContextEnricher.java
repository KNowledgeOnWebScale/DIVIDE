package be.ugent.idlab.divide.core.context;

import be.ugent.idlab.divide.core.engine.DivideOntology;

public class DummyContextEnricher implements IContextEnricher {

    @Override
    public void registerOntology(DivideOntology ontology) {
        // do nothing - empty on purpose
    }

    @Override
    public void enrichContext(Context context) {
        // do nothing - empty on purpose
    }

}
