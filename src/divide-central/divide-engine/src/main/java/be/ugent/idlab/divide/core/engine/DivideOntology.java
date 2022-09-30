package be.ugent.idlab.divide.core.engine;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.reasoner.rulesys.Rule;

import java.util.List;
import java.util.UUID;

public class DivideOntology {

    private final String id;
    private final Model model;
    private final List<Rule> rules;

    public DivideOntology(Model model, List<Rule> rules) {
        this.id = UUID.randomUUID().toString();
        this.model = model;
        this.rules = rules;
    }

    public String getId() {
        return id;
    }

    public Model getModel() {
        return model;
    }

    public List<Rule> getRules() {
        return rules;
    }

}
