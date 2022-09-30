package be.ugent.idlab.divide.core.query.parser;

import be.ugent.idlab.divide.core.context.ContextEnrichment;

import java.util.Set;

public interface IDivideQueryParser {

    DivideQueryParserOutput parseDivideQuery(DivideQueryParserInput input)
            throws InvalidDivideQueryParserInputException;

    void validateDivideQueryContextEnrichment(ContextEnrichment contextEnrichment)
            throws InvalidDivideQueryParserInputException;

    ParsedSparqlQuery parseSparqlQuery(String query)
            throws InvalidDivideQueryParserInputException;

    String getTurtlePrefixList(Set<Prefix> prefixes);

}
