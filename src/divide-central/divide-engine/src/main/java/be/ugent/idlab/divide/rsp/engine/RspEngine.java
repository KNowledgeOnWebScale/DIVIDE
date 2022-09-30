package be.ugent.idlab.divide.rsp.engine;

import be.ugent.idlab.divide.rsp.RspQueryLanguage;
import be.ugent.idlab.divide.rsp.query.IRspQuery;

import java.util.ArrayList;
import java.util.List;

public class RspEngine implements IRspEngine {

    private final RspQueryLanguage rspQueryLanguage;
    private final String baseUrl;
    private final String registrationUrl;
    private final String streamsUrl;
    private final List<IRspQuery> registeredQueries;

    public RspEngine(RspQueryLanguage rspQueryLanguage, String url) {
        this.rspQueryLanguage = rspQueryLanguage;
        this.baseUrl = url;
        String formattedBaseUrl = baseUrl.endsWith("/") ?
                baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.registrationUrl = String.format("%s/queries", formattedBaseUrl);
        this.streamsUrl = String.format("%s/streams", formattedBaseUrl);
        this.registeredQueries = new ArrayList<>();
    }

    @Override
    public synchronized RspQueryLanguage getRspQueryLanguage() {
        return rspQueryLanguage;
    }

    @Override
    public String getBaseUrl() {
        return baseUrl;
    }

    @Override
    public synchronized String getRegistrationUrl() {
        return registrationUrl;
    }

    @Override
    public String getStreamsUrl() {
        return streamsUrl;
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
