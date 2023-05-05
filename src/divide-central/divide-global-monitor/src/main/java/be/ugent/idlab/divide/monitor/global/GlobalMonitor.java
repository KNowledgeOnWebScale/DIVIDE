package be.ugent.idlab.divide.monitor.global;

import be.ugent.idlab.divide.core.engine.IDivideEngine;
import be.ugent.idlab.divide.monitor.IDivideGlobalMonitor;
import be.ugent.idlab.divide.monitor.global.rs.GlobalMonitorReasoningServiceFactory;
import be.ugent.idlab.divide.monitor.metamodel.DivideMetaModelFactory;
import be.ugent.idlab.divide.monitor.metamodel.IDivideMetaModel;
import be.ugent.idlab.divide.util.JarResourceManager;
import be.ugent.idlab.reasoningservice.common.ReasoningServer;
import be.ugent.idlab.reasoningservice.common.configuration.StaticConfig;
import be.ugent.idlab.reasoningservice.common.reasoning.ReasoningService;
import be.ugent.idlab.reasoningservice.jena.JenaReasoningService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

class GlobalMonitor implements IDivideGlobalMonitor {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalMonitor.class.getName());

    private final Map<String, GlobalMonitorThread> threadMap;
    private final Thread.UncaughtExceptionHandler uncaughtExceptionHandler;

    private final IDivideMetaModel divideMetaModel;

    GlobalMonitor(IDivideEngine divideEngine,
                  List<GlobalMonitorQuery> globalMonitorQueries) throws GlobalMonitorException {
        LOGGER.info("Creating DIVIDE Global Monitor with the following global monitor queries:");
        for (GlobalMonitorQuery globalMonitorQuery : globalMonitorQueries) {
            LOGGER.info("Global Monitor query: {}", globalMonitorQuery);
        }

        // initialize thread handling
        this.threadMap = new HashMap<>();
        this.uncaughtExceptionHandler = new GlobalMonitorUncaughtExceptionHandler();

        // create reasoning service, initialize it & register stream
        LOGGER.info("Initializing Jena reasoning service & server for Global Monitor");
        ReasoningService reasoningService = new JenaReasoningService();
        ReasoningServer reasoningServer = new ReasoningServer(reasoningService);
        List<URL> metaModelOntologyURLs = getMetaModelOntologyURLs();
        if (metaModelOntologyURLs == null) {
            throw new GlobalMonitorException("Resource list of meta model ontology files cannot be processed");
        }
        StaticConfig config = GlobalMonitorReasoningServiceFactory.
                getReasoningServiceConfig(metaModelOntologyURLs);
        LOGGER.info("Initializing the Global Monitor's reasoning server with the following config: {}",
                config);
        reasoningServer.initialize(config, null);

        // initialize an empty DIVIDE meta model
        LOGGER.info("Creating DIVIDE Meta Model using the Global Monitor's reasoning service");
        this.divideMetaModel = DivideMetaModelFactory.createInstance(reasoningServer);

        // prepare all global monitor services based on config
        prepareServices(reasoningServer, reasoningService, divideEngine, globalMonitorQueries);
    }

    private void prepareServices(ReasoningServer reasoningServer,
                                 ReasoningService reasoningService,
                                 IDivideEngine divideEngine,
                                 List<GlobalMonitorQuery> globalMonitorQueries) {
        final IGlobalMonitorService globalMonitorService =
                GlobalMonitorReasoningServiceFactory.createGlobalMonitorReasoningService(
                        reasoningServer, reasoningService, divideEngine, globalMonitorQueries);
        createServiceThread(globalMonitorService, "global-monitor-reasoning-service", 0);
    }

    private void createServiceThread(IGlobalMonitorService service, String serviceName, int id) {
        String threadName = String.format("%s_%d", serviceName, id);
        GlobalMonitorThread serviceThread = new GlobalMonitorThread(service, threadName, id);
        this.threadMap.put(serviceName, serviceThread);
    }

    @Override
    public void start() {
        LOGGER.info("Starting DIVIDE Global Monitor");

        // start every global monitor thread
        for (GlobalMonitorThread thread: this.threadMap.values()) {
            thread.start();
        }
    }

    @Override
    public IDivideMetaModel getDivideMetaModel() {
        return divideMetaModel;
    }

    private class GlobalMonitorUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {

        @Override
        public void uncaughtException(Thread thread, Throwable e) {
            LOGGER.error("Uncaught exception in thread {}", thread.getName(), e);
            synchronized (GlobalMonitor.this.threadMap) {
                String serviceName = thread.getName().split("_")[0];
                if (GlobalMonitor.this.threadMap.containsKey(serviceName)) {
                    LOGGER.warn("Restarting service of thread {} in new thread", thread.getName());

                    // retrieve service from old thread and reset service
                    GlobalMonitorThread oldThread = GlobalMonitor.this.threadMap.get(serviceName);
                    IGlobalMonitorService service = oldThread.globalMonitorService;
                    service.reset();

                    // create new thread for this reset service and start
                    createServiceThread(service, serviceName, oldThread.id + 1);
                    GlobalMonitorThread newThread = GlobalMonitor.this.threadMap.get(serviceName);
                    Executors.newScheduledThreadPool(1).schedule(newThread::start, 1, TimeUnit.SECONDS);
                }
            }
        }

    }

    private class GlobalMonitorThread {

        private final IGlobalMonitorService globalMonitorService;
        private final Thread thread;
        private final String name;
        private final int id;

        GlobalMonitorThread(IGlobalMonitorService globalMonitorService,
                            String name,
                            int id) {
            this.globalMonitorService = globalMonitorService;
            this.thread = new Thread(new GlobalMonitorRunnable(globalMonitorService));
            this.thread.setName(name);
            this.thread.setUncaughtExceptionHandler(GlobalMonitor.this.uncaughtExceptionHandler);
            this.name = name;
            this.id = id;
        }

        void start() {
            LOGGER.info("Starting {} thread", this.name);
            this.thread.start();
        }

    }

    private static class GlobalMonitorRunnable implements Runnable {

        private final IGlobalMonitorService globalMonitorService;

        GlobalMonitorRunnable(IGlobalMonitorService globalMonitorService) {
            this.globalMonitorService = globalMonitorService;
        }

        @Override
        public void run() {
            try {
                globalMonitorService.start();
            } catch (GlobalMonitorException e) {
                throw new RuntimeException(e);
            }
        }

    }

    private List<URL> getMetaModelOntologyURLs() {
        List<URL> ontologyURLs = new ArrayList<>();
        String ontologyDirectory = "meta-model/ontology";
        try {
            List<String> files = JarResourceManager.getInstance().readLinesOfResourceFile(
                    Paths.get(ontologyDirectory, "files.txt").toString());
            for (String file : files) {
                String createdFilePath = JarResourceManager.getInstance().copyResourceToFile(
                        Paths.get(ontologyDirectory, file), "ontology");
                ontologyURLs.add(new URL(String.format("file:%s", createdFilePath)));
            }

            return ontologyURLs;

        } catch (IOException e) {
            LOGGER.error("Could not process list of meta model ontology URLs", e);
            return null;
        }
    }

}
