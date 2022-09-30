package be.ugent.idlab.divide.rsp.translate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RspQLQueryTranslator implements IQueryTranslator {

    private static final Logger LOGGER = LoggerFactory.getLogger(RspQLQueryTranslator.class.getName());

    @Override
    public String translateQuery(String queryBody, String queryName) {
        LOGGER.info("Translating query '{}' to RSP-QL syntax (= leaving unchanged)", queryName);

        // RSP-QL is the default format used by DIVIDE, so no translation required
        return queryBody;
    }

}
