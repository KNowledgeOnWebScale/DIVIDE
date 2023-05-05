package be.ugent.idlab.divide.monitor.metamodel;

import be.ugent.idlab.divide.core.component.IComponent;
import be.ugent.idlab.divide.core.engine.IDivideEngine;
import be.ugent.idlab.divide.core.exception.DivideNotInitializedException;
import be.ugent.idlab.divide.core.query.IDivideQuery;
import be.ugent.idlab.divide.monitor.global.GlobalMonitorException;
import be.ugent.idlab.divide.rsp.RspLocation;
import be.ugent.idlab.divide.rsp.engine.IRspEngine;
import be.ugent.idlab.divide.rsp.query.IRspQuery;
import be.ugent.idlab.divide.rsp.query.window.IStreamWindow;
import be.ugent.idlab.divide.util.JarResourceManager;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

class DivideMetaModelMapper {

    private static final Map<RspLocation, String> rspLocationMap = new HashMap<>();
    static {
        rspLocationMap.put(RspLocation.LOCAL, "LocalLocation");
        rspLocationMap.put(RspLocation.CENTRAL, "CentralLocation");
    }

    // used prefixes
    private final String prefixes;

    // triple pattern templates
    private final String divideEngineTemplate;
    private final String divideComponentTemplate;
    private final String divideComponentCentralRspEngineTemplate;
    private final String divideQueryTemplate;
    private final String divideQueryDeploymentTemplate;
    private final String rspQueryTemplate;
    private final String rspQuerySlidingStepTemplate;
    private final String rspQueryWindowSizeTemplate;
    private final String streamWindowTemplate;
    private final String streamWindowSizeWithFromToTemplate;
    private final String streamWindowSizeWithRangeTemplate;

    // URI templates
    private final String deviceUriTemplate;
    private final String divideComponentUriTemplate;
    private final String divideEngineUriTemplate;
    private final String divideQueryUriTemplate;
    private final String divideQueryDeploymentUriTemplate;
    private final String divideQueryDeploymentLocationUriTemplate;
    private final String rdfStreamUriTemplate;
    private final String rspEngineUriTemplate;
    private final String rspQueryUriTemplate;
    private final String streamWindowUriTemplate;

    DivideMetaModelMapper() throws GlobalMonitorException {
        String templateDirectory = Paths.get("meta-model", "mapper-templates").toString();
        String triplePatternTemplateDirectory = "triple-patterns";
        String uriTemplateDirectory = "uris";
        try {
            this.prefixes = JarResourceManager.getInstance().readResourceFile(
                    Paths.get(templateDirectory, "prefixes.ttl").toString());

            this.divideEngineTemplate = JarResourceManager.getInstance().readResourceFile(
                    Paths.get(templateDirectory, triplePatternTemplateDirectory,
                            "divide-engine.ttl").toString());
            this.divideComponentTemplate = JarResourceManager.getInstance().readResourceFile(
                    Paths.get(templateDirectory, triplePatternTemplateDirectory,
                            "divide-component.ttl").toString());
            this.divideComponentCentralRspEngineTemplate = JarResourceManager.getInstance().readResourceFile(
                    Paths.get(templateDirectory, triplePatternTemplateDirectory,
                            "divide-component-central-rsp-engine.ttl").toString());
            this.divideQueryTemplate = JarResourceManager.getInstance().readResourceFile(
                    Paths.get(templateDirectory, triplePatternTemplateDirectory,
                            "divide-query.ttl").toString());
            this.divideQueryDeploymentTemplate = JarResourceManager.getInstance().readResourceFile(
                    Paths.get(templateDirectory, triplePatternTemplateDirectory,
                            "divide-query-deployment.ttl").toString());
            this.rspQueryTemplate = JarResourceManager.getInstance().readResourceFile(
                    Paths.get(templateDirectory, triplePatternTemplateDirectory,
                            "rsp-query.ttl").toString());
            this.rspQuerySlidingStepTemplate = JarResourceManager.getInstance().readResourceFile(
                    Paths.get(templateDirectory, triplePatternTemplateDirectory,
                            "rsp-query-sliding-step.ttl").toString());
            this.rspQueryWindowSizeTemplate = JarResourceManager.getInstance().readResourceFile(
                    Paths.get(templateDirectory, triplePatternTemplateDirectory,
                            "rsp-query-window-size.ttl").toString());
            this.streamWindowTemplate = JarResourceManager.getInstance().readResourceFile(
                    Paths.get(templateDirectory, triplePatternTemplateDirectory,
                            "stream-window.ttl").toString());
            this.streamWindowSizeWithFromToTemplate = JarResourceManager.getInstance().readResourceFile(
                    Paths.get(templateDirectory, triplePatternTemplateDirectory,
                            "stream-window-size-from-to.ttl").toString());
            this.streamWindowSizeWithRangeTemplate = JarResourceManager.getInstance().readResourceFile(
                    Paths.get(templateDirectory, triplePatternTemplateDirectory,
                            "stream-window-size-range.ttl").toString());

            this.deviceUriTemplate = JarResourceManager.getInstance().readResourceFile(
                    Paths.get(templateDirectory, uriTemplateDirectory, "device").toString());
            this.divideComponentUriTemplate = JarResourceManager.getInstance().readResourceFile(
                    Paths.get(templateDirectory, uriTemplateDirectory, "divide-component").toString());
            this.divideEngineUriTemplate = JarResourceManager.getInstance().readResourceFile(
                    Paths.get(templateDirectory, uriTemplateDirectory, "divide-engine").toString());
            this.divideQueryUriTemplate = JarResourceManager.getInstance().readResourceFile(
                    Paths.get(templateDirectory, uriTemplateDirectory, "divide-query").toString());
            this.divideQueryDeploymentUriTemplate = JarResourceManager.getInstance().readResourceFile(
                    Paths.get(templateDirectory, uriTemplateDirectory, "divide-query-deployment").toString());
            this.divideQueryDeploymentLocationUriTemplate = JarResourceManager.getInstance().readResourceFile(
                    Paths.get(templateDirectory, uriTemplateDirectory, "divide-query-deployment-location").toString());
            this.rdfStreamUriTemplate = JarResourceManager.getInstance().readResourceFile(
                    Paths.get(templateDirectory, uriTemplateDirectory, "rdf-stream").toString());
            this.rspEngineUriTemplate = JarResourceManager.getInstance().readResourceFile(
                    Paths.get(templateDirectory, uriTemplateDirectory, "rsp-engine").toString());
            this.rspQueryUriTemplate = JarResourceManager.getInstance().readResourceFile(
                    Paths.get(templateDirectory, uriTemplateDirectory, "rsp-query").toString());
            this.streamWindowUriTemplate = JarResourceManager.getInstance().readResourceFile(
                    Paths.get(templateDirectory, uriTemplateDirectory, "stream-window").toString());

        } catch (IOException e) {
            throw new GlobalMonitorException("Could not initialize templates of DIVIDE Meta Model mapper");
        }
    }

    String getDivideEngineTriples(IDivideEngine divideEngine) {
        String triples = "";
        try {
            triples = String.format(this.divideEngineTemplate,
                    getDivideEngineUri(divideEngine),
                    divideEngine.getId(),
                    getDeviceUri(divideEngine),
                    getDeviceUri(divideEngine),
                    divideEngine.getDeviceNetworkIp());
        } catch (DivideNotInitializedException e) {
            // will not happen
        }

        return String.format("%s\n%s", this.prefixes, triples);
    }

    String getDivideComponentTriples(IComponent component) {
        String triples = String.format(this.divideComponentTemplate,
                getDivideComponentUri(component),
                component.getId(),
                getDeviceUri(component),
                getRspEngineUri(component.getRspEngineHandler().getLocalRspEngine()),
                getRspEngineUri(component.getRspEngineHandler().getLocalRspEngine()),
                component.getRspEngineHandler().getLocalRspEngine().getServerPort(),
                getDeviceUri(component),
                component.getIpAddress());

        // append with central RSP engine information if it is configured
        if (component.getRspEngineHandler().getCentralRspEngine() != null) {
            triples += "\n" + String.format(divideComponentCentralRspEngineTemplate,
                    getDivideComponentUri(component),
                    getRspEngineUri(component.getRspEngineHandler().getCentralRspEngine()));
        }

        return String.format("%s\n%s", this.prefixes, triples);
    }

    String getDivideQueryTriples(IDivideQuery divideQuery) {
        String triples = String.format(this.divideQueryTemplate,
                getDivideQueryUri(divideQuery),
                divideQuery.getName());

        return String.format("%s\n%s", this.prefixes, triples);
    }

    String getDivideQueryDeploymentTriples(IDivideQuery divideQuery,
                                           IComponent component,
                                           RspLocation rspLocation) {
        String triples = String.format(this.divideQueryDeploymentTemplate,
                getDivideQueryUri(divideQuery),
                getDivideQueryDeploymentUri(divideQuery, component),
                getDivideQueryDeploymentUri(divideQuery, component),
                getDivideComponentUri(component),
                getDivideQueryDeploymentLocationUri(divideQuery, component, rspLocation),
                getDivideQueryDeploymentLocationUri(divideQuery, component, rspLocation),
                rspLocationMap.get(rspLocation));

        return String.format("%s\n%s", this.prefixes, triples);
    }

    String getRspQueryTriples(IRspQuery rspQuery) {
        // generate triples for RSP query
        String queryTriples = String.format(this.rspQueryTemplate,
                getRspQueryUri(rspQuery.getRspEngine(), rspQuery),
                rspQuery.getId(),
                rspQuery.getQueryName(),
                rspQuery.getStreamWindows().stream()
                        .map(s -> {
                            try {
                                return getStreamWindowUri(s, rspQuery, rspQuery.getRspEngine());
                            } catch (UnsupportedEncodingException e) {
                                // will not happen if streams are valid
                                return null;
                            }
                        })
                        .filter(Objects::nonNull)
                        .collect(Collectors.joining(", ")),
                getRspEngineUri(rspQuery.getRspEngine()),
                getDivideQueryUri(rspQuery.getOriginalDivideQuery()),
                getDivideComponentUri(rspQuery.getAssociatedComponent()));

        // append with query sliding step & window size if available
        if (rspQuery.getQuerySlidingStepInSeconds() != -1) {
            queryTriples += "\n" + String.format(rspQuerySlidingStepTemplate,
                    getRspQueryUri(rspQuery.getRspEngine(), rspQuery),
                    rspQuery.getQuerySlidingStepInSeconds());
        }
        if (rspQuery.getStreamWindowSizeInSeconds() != -1) {
            queryTriples += "\n" + String.format(rspQueryWindowSizeTemplate,
                    getRspQueryUri(rspQuery.getRspEngine(), rspQuery),
                    rspQuery.getStreamWindowSizeInSeconds());
        }

        // generate additional triples for stream windows
        String streamWindowTriples = "";
        for (IStreamWindow streamWindow : rspQuery.getStreamWindows()) {
            try {
                streamWindowTriples = "\n" + String.format(streamWindowTemplate,
                        getStreamWindowUri(streamWindow, rspQuery, rspQuery.getRspEngine()),
                        getRdfStreamUri(rspQuery.getRspEngine(), streamWindow.getStreamIri()),
                        streamWindow.getWindowDefinition(),
                        streamWindow.getQuerySlidingStepInSeconds(),
                        getRdfStreamUri(rspQuery.getRspEngine(), streamWindow.getStreamIri()),
                        streamWindow.getStreamIri());

                if (streamWindow.getWindowSizeInSeconds() != -1) {
                    // stream window with a single window size
                    streamWindowTriples += "\n" + String.format(streamWindowSizeWithRangeTemplate,
                            getStreamWindowUri(streamWindow, rspQuery, rspQuery.getRspEngine()),
                            streamWindow.getWindowSizeInSeconds());
                } else {
                    // stream window with a FROM & TO specification
                    streamWindowTriples += "\n" + String.format(streamWindowSizeWithFromToTemplate,
                            getStreamWindowUri(streamWindow, rspQuery, rspQuery.getRspEngine()),
                            streamWindow.getWindowStartInSecondsAgo(),
                            streamWindow.getWindowEndInSecondsAgo());
                }
            } catch (UnsupportedEncodingException e) {
                // will not happen
            }
        }

        return String.format("%s\n%s\n%s", this.prefixes, queryTriples, streamWindowTriples);
    }


    // construction of URIs

    private String getDeviceUri(IDivideEngine divideEngine) throws DivideNotInitializedException {
        return String.format(deviceUriTemplate, getUrlIdOfDevice(divideEngine));
    }

    private String getDeviceUri(IComponent component) {
        return String.format(deviceUriTemplate, getUrlIdOfDevice(component));
    }

    private String getDivideComponentUri(IComponent component) {
        return String.format(divideComponentUriTemplate, getUrlId(component));
    }

    private String getDivideEngineUri(IDivideEngine divideEngine) {
        return String.format(divideEngineUriTemplate, getUrlId(divideEngine));
    }

    private String getDivideQueryUri(IDivideQuery divideQuery) {
        return String.format(divideQueryUriTemplate, getUrlId(divideQuery));
    }

    private String getDivideQueryDeploymentUri(IDivideQuery divideQuery,
                                               IComponent component) {
        return String.format(divideQueryDeploymentUriTemplate,
                getUrlId(divideQuery), getUrlId(component));
    }

    private String getDivideQueryDeploymentLocationUri(IDivideQuery divideQuery,
                                                       IComponent component,
                                                       RspLocation location) {
        return String.format(divideQueryDeploymentLocationUriTemplate,
                getUrlId(divideQuery), getUrlId(component),
                rspLocationMap.get(location));
    }

    private String getRdfStreamUri(IRspEngine rspEngine,
                                   String streamName) throws UnsupportedEncodingException {
        return String.format(rdfStreamUriTemplate, getUrlId(rspEngine),
                URLEncoder.encode(streamName, StandardCharsets.UTF_8.toString()));
    }

    private String getRspEngineUri(IRspEngine rspEngine) {
        return String.format(rspEngineUriTemplate, getUrlId(rspEngine));
    }

    private String getRspQueryUri(IRspEngine rspEngine,
                                  IRspQuery rspQuery) {
        return String.format(rspQueryUriTemplate, getUrlId(rspEngine), getUrlId(rspQuery));
    }

    private String getStreamWindowUri(IStreamWindow streamWindow,
                                      IRspQuery rspQuery,
                                      IRspEngine rspEngine) throws UnsupportedEncodingException {
        return String.format(streamWindowUriTemplate,
                getUrlId(rspEngine), getUrlId(rspQuery), getUrlId(streamWindow));
    }


    // retrieval of IDs in URIs

    private String getUrlId(IDivideEngine divideEngine) {
        return divideEngine.getId();
    }

    private String getUrlIdOfDevice(IDivideEngine divideEngine) throws DivideNotInitializedException {
        return divideEngine.getDeviceNetworkIp();
    }

    private String getUrlIdOfDevice(IComponent component) {
        return component.getIpAddress();
    }

    private String getUrlId(IComponent component) {
        return component.getId();
    }

    private String getUrlId(IRspEngine rspEngine) {
        return rspEngine.getId();
    }

    private String getUrlId(IDivideQuery divideQuery) {
        return divideQuery.getName();
    }

    private String getUrlId(IRspQuery rspQuery) {
        return rspQuery.getId();
    }

    private String getUrlId(IStreamWindow streamWindow) throws UnsupportedEncodingException {
        return String.format("%s-%s",
                URLEncoder.encode(streamWindow.getStreamIri(), StandardCharsets.UTF_8.toString()),
                URLEncoder.encode(streamWindow.getWindowDefinition(), StandardCharsets.UTF_8.toString()));
    }

}
