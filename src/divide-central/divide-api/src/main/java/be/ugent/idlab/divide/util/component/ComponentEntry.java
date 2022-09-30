package be.ugent.idlab.divide.util.component;

import be.ugent.idlab.divide.rsp.RspQueryLanguage;

import java.util.List;

public class ComponentEntry {

    private final List<String> contextIris;
    private final RspQueryLanguage rspQueryLanguage;
    private final String rspEngineUrl;

    public ComponentEntry(List<String> contextIris,
                          RspQueryLanguage rspQueryLanguage,
                          String rspEngineUrl) {
        this.contextIris = contextIris;
        this.rspQueryLanguage = rspQueryLanguage;
        this.rspEngineUrl = rspEngineUrl;
    }

    public List<String> getContextIris() {
        return contextIris;
    }

    public RspQueryLanguage getRspQueryLanguage() {
        return rspQueryLanguage;
    }

    public String getRspEngineUrl() {
        return rspEngineUrl;
    }

}
