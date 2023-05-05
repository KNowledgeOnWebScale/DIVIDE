package be.ugent.idlab.divide.rsp.engine;

import be.ugent.idlab.divide.rsp.RspQueryLanguage;
import be.ugent.idlab.divide.rsp.query.IRspQuery;

import java.util.ArrayList;
import java.util.List;

public class RspEngine implements IRspEngine {

    private final RspQueryLanguage rspQueryLanguage;
    private final String baseUrl;
    private final int serverPort;
    private final List<IRspQuery> registeredQueries;
    private final String id;

    private String webSocketStreamUrl;

    public RspEngine(RspQueryLanguage rspQueryLanguage,
                     String url,
                     int serverPort,
                     String componentId) {
        this.rspQueryLanguage = rspQueryLanguage;
        this.baseUrl = url.endsWith("/") ?
                url.substring(0, url.length() - 1) : url;
        this.serverPort = serverPort;
        this.registeredQueries = new ArrayList<>();

        // ID of RSP engine is identical to component ID
        // (due to 1 on 1 mapping between component & local RSP engine)
        this.id = componentId;
    }

    @Override
    public synchronized String getId() {
        return id;
    }

    @Override
    public synchronized RspQueryLanguage getRspQueryLanguage() {
        return rspQueryLanguage;
    }

    @Override
    public synchronized String getBaseUrl() {
        return baseUrl;
    }

    @Override
    public synchronized int getServerPort() {
        return serverPort;
    }

    @Override
    public synchronized void setWebSocketStreamUrl(String webSocketStreamUrl) {
        this.webSocketStreamUrl = webSocketStreamUrl.endsWith("/") ?
                webSocketStreamUrl.substring(0, webSocketStreamUrl.length() - 1)
                : webSocketStreamUrl;
    }

    @Override
    public synchronized String getWebSocketStreamUrl() {
        return webSocketStreamUrl;
    }

    @Override
    public synchronized List<IRspQuery> getRegisteredQueries() {
        return registeredQueries;
    }

    @Override
    public synchronized void addRegisteredQuery(IRspQuery query) {
        registeredQueries.add(query);
    }

    @Override
    public synchronized void removeRegisteredQuery(IRspQuery query) {
        registeredQueries.remove(query);
    }

}
