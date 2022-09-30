package be.ugent.idlab.divide.rsp.translate;

import be.ugent.idlab.divide.rsp.RspQueryLanguage;

/**
 * Translator capable of translating an RSP-QL query to a specific
 * {@link RspQueryLanguage}.
 */
public interface IQueryTranslator {

    /**
     * Translates an RSP-QL query with the given query body and query name
     * to the {@link RspQueryLanguage} of this translator.
     *
     * @param queryBody body of the query to be translated
     * @param queryName name of the query to be translated
     * @return query body of the translated query
     */
    String translateQuery(String queryBody, String queryName);

}
