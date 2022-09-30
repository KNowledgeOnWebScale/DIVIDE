package be.ugent.idlab.divide.util.query;

import be.ugent.idlab.divide.core.query.parser.DivideQueryParserInput;

public class DivideQueryEntryInQueryFormat {

    DivideQueryParserInput divideQueryParserInput;
    ContextEnrichmentEntry contextEnrichmentEntry;

    public DivideQueryEntryInQueryFormat(DivideQueryParserInput divideQueryParserInput,
                                         ContextEnrichmentEntry contextEnrichmentEntry) {
        this.divideQueryParserInput = divideQueryParserInput;
        this.contextEnrichmentEntry = contextEnrichmentEntry;
    }

    public DivideQueryParserInput getDivideQueryParserInput() {
        return divideQueryParserInput;
    }

    public ContextEnrichmentEntry getContextEnrichmentEntry() {
        return contextEnrichmentEntry;
    }

}
