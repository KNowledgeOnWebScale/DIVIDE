package be.ugent.idlab.divide.util.component;

import java.util.List;

@SuppressWarnings("unused")
public class JsonComponentEntry {

    private List<String> contextIris;
    private RspEngineEntry rspEngine;

    public JsonComponentEntry(List<String> contextIris, RspEngineEntry rspEngine) {
        this.contextIris = contextIris;
        this.rspEngine = rspEngine;
    }

    public List<String> getContextIris() {
        return contextIris;
    }

    public void setContextIris(List<String> contextIris) {
        this.contextIris = contextIris;
    }

    public RspEngineEntry getRspEngine() {
        return rspEngine;
    }

    public void setRspEngine(RspEngineEntry rspEngine) {
        this.rspEngine = rspEngine;
    }

    public boolean validateIfNonNull() {
        return contextIris != null &&
                rspEngine != null &&
                rspEngine.queryLanguage != null &&
                rspEngine.url != null;
    }

    static class RspEngineEntry {

        public RspEngineEntry(String queryLanguage, String url) {
            this.queryLanguage = queryLanguage;
            this.url = url;
        }

        private String queryLanguage;
        private String url;

        public String getQueryLanguage() {
            return queryLanguage;
        }

        public void setQueryLanguage(String queryLanguage) {
            this.queryLanguage = queryLanguage;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

    }

}
