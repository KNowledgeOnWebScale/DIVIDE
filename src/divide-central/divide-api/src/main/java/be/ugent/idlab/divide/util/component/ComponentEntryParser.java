package be.ugent.idlab.divide.util.component;

import be.ugent.idlab.divide.rsp.RspQueryLanguage;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

public class ComponentEntryParser {

    static void validateContextIris(List<String> contextIris)
            throws ComponentEntryParserException{
        for (String contextIri : contextIris) {
            if (contextIri == null || contextIri.trim().isEmpty()) {
                throw new ComponentEntryParserException(
                        "Component entry contains empty context IRIs");
            }
        }
    }

    static RspQueryLanguage parseRspEngineQueryLanguage(String input)
            throws ComponentEntryParserException {
        RspQueryLanguage rspQueryLanguage = RspQueryLanguage.fromString(input.trim());
        if (rspQueryLanguage == null) {
            throw new ComponentEntryParserException(String.format(
                    "Component entry contains invalid/unsupported RSP query language '%s'",
                    input));
        }
        return rspQueryLanguage;
    }

    static void validateRspEngineUrl(String rspEngineUrl)
            throws ComponentEntryParserException {
        try {
            URL url = new URL(rspEngineUrl);
            if (!url.getProtocol().equals("http") && !url.getProtocol().equals("https")) {
                throw new ComponentEntryParserException(String.format(
                        "Component entry contains non HTTP(S) RSP engine URL '%s'",
                        rspEngineUrl));
            }
        } catch (MalformedURLException e) {
            throw new ComponentEntryParserException(String.format(
                    "Component entry contains invalid RSP engine URL '%s'",
                    rspEngineUrl));
        }
    }

}
