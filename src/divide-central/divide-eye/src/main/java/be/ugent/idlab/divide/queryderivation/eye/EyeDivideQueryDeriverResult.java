package be.ugent.idlab.divide.queryderivation.eye;

import be.ugent.idlab.divide.core.engine.IDivideQueryDeriverResult;
import be.ugent.idlab.util.rdf.RDFLanguage;
import be.ugent.idlab.util.rdf.jena3.owlapi4.JenaUtilities;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;

import java.util.ArrayList;
import java.util.List;

public class EyeDivideQueryDeriverResult implements IDivideQueryDeriverResult {

    private final EyeDivideQueryDeriverIntermediateResult intermediateResult;
    private final String queriesAfterStaticWindowParameterSubstitution;
    private final Model substitutedQueriesModel;
    private final List<String> substitutedRspQlQueries;

    public EyeDivideQueryDeriverResult() {
        this.intermediateResult = null;
        this.queriesAfterStaticWindowParameterSubstitution = null;
        this.substitutedQueriesModel = ModelFactory.createDefaultModel();
        this.substitutedRspQlQueries = new ArrayList<>();
    }

    public EyeDivideQueryDeriverResult(EyeDivideQueryDeriverIntermediateResult intermediateResult,
                                       String queriesAfterStaticWindowParameterSubstitution,
                                       Model substitutedQueriesModel,
                                       List<String> substitutedRspQlQueries) {
        this.intermediateResult = intermediateResult;
        this.queriesAfterStaticWindowParameterSubstitution =
                queriesAfterStaticWindowParameterSubstitution;
        this.substitutedQueriesModel = substitutedQueriesModel;
        this.substitutedRspQlQueries = substitutedRspQlQueries;
    }

    public EyeDivideQueryDeriverIntermediateResult getIntermediateResult() {
        return intermediateResult;
    }

    public String getQueriesAfterStaticWindowParameterSubstitution() {
        return queriesAfterStaticWindowParameterSubstitution;
    }

    public Model getSubstitutedQueriesModel() {
        return substitutedQueriesModel;
    }

    @Override
    public List<String> getSubstitutedRspQlQueries() {
        return substitutedRspQlQueries;
    }

    @Override
    public String toString() {
        return "EyeDivideQueryDeriverResult{" +
                "intermediateResult=" + intermediateResult +
                ", queriesAfterStaticWindowParameterSubstitution='" +
                queriesAfterStaticWindowParameterSubstitution + '\'' +
                ", substitutedQueriesModel=" + JenaUtilities.serializeModel(
                        substitutedQueriesModel, RDFLanguage.TURTLE) +
                ", substitutedRspQlQueries=" + substitutedRspQlQueries +
                '}';
    }

}
