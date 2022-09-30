package be.ugent.idlab.divide.rsp;

/**
 * RSP query languages that are supported by DIVIDE.
 * Currently, DIVIDE supports RSP-QL ({@link #RSP_QL}) and C-SPARQL ({@link #CSPARQL}) queries.
 */
public enum RspQueryLanguage {

    RSP_QL,
    CSPARQL;

    /**
     * @param name case insensitive name of RSP query language to retrieve
     * @return {@link RspQueryLanguage} of which the name matches the given
     *         name (case insensitive); null if no match
     */
    public static RspQueryLanguage fromString(String name) {
        for (RspQueryLanguage language : RspQueryLanguage.values()) {
            if (language.name().equalsIgnoreCase(name)) {
                return language;
            }
        }
        return null;
    }

}
