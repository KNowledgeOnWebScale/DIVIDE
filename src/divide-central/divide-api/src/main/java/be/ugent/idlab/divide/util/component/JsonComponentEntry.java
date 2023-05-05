package be.ugent.idlab.divide.util.component;

import java.util.List;

@SuppressWarnings("unused")
public class JsonComponentEntry {

    private String ipAddress;
    private List<String> contextIris;
    private RspEngineEntry rspEngine;

    public JsonComponentEntry(String ipAddress,
                              List<String> contextIris,
                              RspEngineEntry rspEngine) {
        this.ipAddress = ipAddress;
        this.contextIris = contextIris;
        this.rspEngine = rspEngine;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
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
        return ipAddress != null &&
                contextIris != null &&
                rspEngine != null &&
                rspEngine.queryLanguage != null &&
                rspEngine.serverPort != 0;
    }

    static class RspEngineEntry {

        public RspEngineEntry(String queryLanguage, int serverPort) {
            this.queryLanguage = queryLanguage;
            this.serverPort = serverPort;
        }

        private String queryLanguage;
        private int serverPort;

        public String getQueryLanguage() {
            return queryLanguage;
        }

        public void setQueryLanguage(String queryLanguage) {
            this.queryLanguage = queryLanguage;
        }

        public int getServerPort() {
            return serverPort;
        }

        public void setServerPort(int serverPort) {
            this.serverPort = serverPort;
        }

    }

}
