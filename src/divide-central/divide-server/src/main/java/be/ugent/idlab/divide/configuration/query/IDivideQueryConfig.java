package be.ugent.idlab.divide.configuration.query;

import java.util.List;

public interface IDivideQueryConfig {

    boolean getContextEnrichmentDoReasoning();

    boolean getContextEnrichmentExecuteOnOntologyTriples();

    List<String> getContextEnrichmentQueryFilePaths();

}
