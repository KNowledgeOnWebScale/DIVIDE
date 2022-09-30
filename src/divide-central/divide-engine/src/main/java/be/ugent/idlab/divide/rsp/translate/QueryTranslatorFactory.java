package be.ugent.idlab.divide.rsp.translate;

import be.ugent.idlab.divide.rsp.RspQueryLanguage;

public class QueryTranslatorFactory {

    /**
     * Returns an {@link IQueryTranslator} which can translate an RSP-QL query string
     * (= internal DIVIDE representation) to the specified RSP query language
     * @param rspQueryLanguage query language to which the query translator should
     *                         translate the RSP-QL queries
     * @return {@link IQueryTranslator} which can translate an RSP-QL query string
     *         to the specified RSP query language
     */
    public static IQueryTranslator createInstance(RspQueryLanguage rspQueryLanguage) {
        switch(rspQueryLanguage) {
            case CSPARQL:
                return new CSparqlQueryTranslator();

            case RSP_QL:
                return new RspQLQueryTranslator();

            default:
                throw new IllegalArgumentException("No valid RSP query language given");
        }
    }

}
