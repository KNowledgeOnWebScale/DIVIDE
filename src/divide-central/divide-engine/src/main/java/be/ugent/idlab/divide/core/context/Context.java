package be.ugent.idlab.divide.core.context;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;

import java.util.UUID;

public class Context {

    private final String id;
    private Model context;

    private boolean enriched;

    public Context(Model context) {
        this.id = UUID.randomUUID().toString();
        this.context = context;
        this.enriched = false;
    }

    Context(String id, Model context) {
        this.id = id;
        this.context = context;
    }

    public String getId() {
        return id;
    }

    public Model getContext() {
        return context;
    }

    public void enrichContext(Model context) {
        if (!enriched) {
            this.context = context;
            enriched = true;
        } else {
            throw new RuntimeException(String.format(
                    "Context with ID '%s' has already been enriched", id));
        }
    }

    public long size() {
        return context.size();
    }

    public Context copy() {
        Model newModel = ModelFactory.createDefaultModel();
        newModel.add(context.listStatements());
        return new Context(newModel);
    }

}
