package be.ugent.idlab.divide.util.component;

import be.ugent.idlab.divide.rsp.RspQueryLanguage;

import java.util.List;

public class ComponentEntry {

    private final String ipAddress;
    private final List<String> contextIris;
    private final RspQueryLanguage rspQueryLanguage;
    private final int rspEngineServerPort;

    public ComponentEntry(String ipAddress,
                          List<String> contextIris,
                          RspQueryLanguage rspQueryLanguage,
                          int rspEngineServerPort) {
        this.ipAddress = ipAddress;
        this.contextIris = contextIris;
        this.rspQueryLanguage = rspQueryLanguage;
        this.rspEngineServerPort = rspEngineServerPort;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public List<String> getContextIris() {
        return contextIris;
    }

    public RspQueryLanguage getRspQueryLanguage() {
        return rspQueryLanguage;
    }

    public int getRspEngineServerPort() {
        return rspEngineServerPort;
    }

}
