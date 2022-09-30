package be.ugent.idlab.divide.core.context;

import be.ugent.idlab.divide.core.engine.DivideOntology;

public interface IContextEnricher {

    void registerOntology(DivideOntology ontology);

    void enrichContext(Context context);

}
