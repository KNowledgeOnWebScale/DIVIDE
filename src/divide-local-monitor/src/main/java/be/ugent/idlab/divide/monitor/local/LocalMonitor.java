package be.ugent.idlab.divide.monitor.local;

import be.ugent.idlab.divide.monitor.local.config.ILocalMonitorConfig;
import be.ugent.idlab.divide.monitor.local.config.LocalMonitorConfigFactory;
import be.ugent.idlab.divide.monitor.local.monitoring.device.DeviceMonitorFactory;
import be.ugent.idlab.divide.monitor.local.monitoring.network.NetworkMonitorFactory;
import be.ugent.idlab.divide.monitor.local.monitoring.rsp.RspMonitorFactory;
import be.ugent.idlab.divide.monitor.local.rsp.LocalMonitorRspEngineFactory;
import be.ugent.idlab.divide.monitor.local.semanticmapper.SemanticMapper;
import be.ugent.idlab.divide.monitor.local.util.JarResourceManager;
import be.ugent.idlab.rspservice.common.RSPServer;
import be.ugent.idlab.rspservice.common.interfaces.RDFStream;
import be.ugent.idlab.rspservice.common.interfaces.RDFStreamProcessor;
import be.ugent.idlab.rspservice.csparql.jena3.CsparqlEngine;
import be.ugent.idlab.rspservice.csparql.jena3.CsparqlServer;
import be.ugent.idlab.util.process.ProcessException;
import be.ugent.idlab.util.process.ProcessUtilities;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class LocalMonitor {

    private static final Logger LOGGER = LoggerFactory.getLogger(LocalMonitor.class.getName());

    public static void main(String[] args) throws LocalMonitorException {
        // parse config file & initialize configuration properties
        ILocalMonitorConfig config;
        try {
            LocalMonitorConfigFactory.initializeFromFile(args[0]);
            config = LocalMonitorConfigFactory.getInstance();
        } catch (ConfigurationException | FileNotFoundException e) {
            LOGGER.error("Error while reading the configuration file {}: " +
                    "specified configuration file does not exist or is not valid", args[0], e);
            return;
        } catch (ArrayIndexOutOfBoundsException e) {
            LOGGER.error("Error while starting local monitor: no configuration file specified");
            return;
        }

        try {
            // create a Local Monitor instance
            LocalMonitor localMonitor = new LocalMonitor(config);

            // start all services of the local monitor
            localMonitor.start();

        } catch (LocalMonitorException e) {
            LOGGER.error("Local monitor could not be started", e);
        }
    }

    private final ILocalMonitorConfig config;
    private final Map<String, LocalMonitorThread> threadMap;
    private final Thread.UncaughtExceptionHandler uncaughtExceptionHandler;

    LocalMonitor(ILocalMonitorConfig config) throws LocalMonitorException {
        LOGGER.info("Starting local monitor with following config: {}", config.toString());
        this.config = config;
        this.threadMap = new HashMap<>();
        this.uncaughtExceptionHandler = new LocalMonitorUncaughtExceptionHandler();

        // install all python requirements
        installPythonRequirements();

        // initialize the semantic mapper
        SemanticMapper.initialize();

        // create C-SPARQL engine, initialize it & register stream
        CsparqlEngine rdfStreamProcessor = new CsparqlEngine();
        RSPServer rspServer = new CsparqlServer(rdfStreamProcessor);
        rspServer.initialize(LocalMonitorRspEngineFactory.getRspEngineConfig());
        RDFStream localMonitorStream = rdfStreamProcessor.registerStream(
                LocalMonitorConstants.RSP_STREAM_IRI);

        // get additional service properties
        String centralHost = config.getCentralMonitorReasoningServiceHost();
        String publicNetworkInterface = config.getLocalPublicNetworkInterface();
        String componentId = config.getComponentId();
        String deviceId = config.getDeviceId();

        // do not start the monitor if any of the additional service properties is not defined
        if (publicNetworkInterface == null || componentId == null || deviceId == null) {
            throw new LocalMonitorException("Local Monitor cannot be started since at " +
                    "least one of the following properties is undefined: " +
                    "public network interface, component ID, device ID");
        }

        // prepare all local monitor services based on config
        prepareServices(rspServer, rdfStreamProcessor, localMonitorStream,
                centralHost, publicNetworkInterface, componentId, deviceId);
    }

    private void installPythonRequirements() throws LocalMonitorException {
        try {
            LOGGER.info("Installing Python requirements for Local Monitor");
            String requirementsPath = JarResourceManager.getInstance().copyResourceToFile(
                    Paths.get("python-requirements.txt"));
            String output = ProcessUtilities.executeProcess(new String[]{
                    "python3", "-m", "pip", "install", "-r", requirementsPath});
            LOGGER.info("Output of installing Python requirements: {}", output);
        } catch (ProcessException | IOException e) {
            throw new LocalMonitorException("Could not install Python requirements for Local Monitor", e);
        }
    }

    private void prepareServices(RSPServer rspServer,
                                 RDFStreamProcessor rdfStreamProcessor,
                                 RDFStream localMonitorStream,
                                 String centralHost,
                                 String publicNetworkInterface,
                                 String componentId,
                                 String deviceId) {
        final ILocalMonitorService rspEngine =
                LocalMonitorRspEngineFactory.createLocalMonitorRspEngine(
                        this.config, rspServer, rdfStreamProcessor);
        createServiceThread(rspEngine, "rsp-engine", 0);

        if (config.shouldStartDeviceMonitor()) {
            final ILocalMonitorService deviceMonitor =
                    DeviceMonitorFactory.createDeviceMonitor(
                            rdfStreamProcessor, localMonitorStream,
                            componentId, deviceId);
            createServiceThread(deviceMonitor, "device-monitor", 0);
        }
        if (config.shouldStartNetworkMonitor()) {
            final ILocalMonitorService networkMonitor =
                    NetworkMonitorFactory.createNetworkMonitor(
                            rdfStreamProcessor, localMonitorStream,
                            componentId, deviceId, publicNetworkInterface, centralHost);
            createServiceThread(networkMonitor, "network-monitor", 0);
        }
        if (config.shouldStartRspMonitor()) {
            final ILocalMonitorService rspMonitor =
                    RspMonitorFactory.createRspMonitor(rdfStreamProcessor, localMonitorStream,
                            componentId, deviceId, config.getWebSocketPortOfLocalRspEngineMonitor());
            createServiceThread(rspMonitor, "rsp-monitor", 0);
        }
    }

    private void createServiceThread(ILocalMonitorService service, String serviceName, int id) {
        String threadName = String.format("%s_%d", serviceName, id);
        LocalMonitorThread serviceThread = new LocalMonitorThread(service, threadName, id);
        this.threadMap.put(serviceName, serviceThread);
    }

    private void start() {
        // start every local monitor thread
        for (LocalMonitorThread thread: this.threadMap.values()) {
            thread.start();
        }
    }

    private class LocalMonitorUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {

        @Override
        public void uncaughtException(Thread thread, Throwable e) {
            LOGGER.error("Uncaught exception in thread {}", thread.getName(), e);
            synchronized (LocalMonitor.this.threadMap) {
                String serviceName = thread.getName().split("_")[0];
                if (LocalMonitor.this.threadMap.containsKey(serviceName)) {
                    LOGGER.warn("Restarting service of thread {} in new thread", thread.getName());

                    // retrieve service from old thread and reset service
                    LocalMonitorThread oldThread = LocalMonitor.this.threadMap.get(serviceName);
                    ILocalMonitorService service = oldThread.localMonitorService;
                    service.reset();

                    // create new thread for this reset service and start
                    createServiceThread(service, serviceName, oldThread.id + 1);
                    LocalMonitorThread newThread = LocalMonitor.this.threadMap.get(serviceName);
                    Executors.newScheduledThreadPool(1).schedule(newThread::start, 1, TimeUnit.SECONDS);
                }
            }
        }

    }

    private class LocalMonitorThread {

        private final ILocalMonitorService localMonitorService;
        private final Thread thread;
        private final String name;
        private final int id;

        LocalMonitorThread(ILocalMonitorService localMonitorService,
                           String name,
                           int id) {
            this.localMonitorService = localMonitorService;
            this.thread = new Thread(new LocalMonitorRunnable(localMonitorService));
            this.thread.setName(name);
            this.thread.setUncaughtExceptionHandler(LocalMonitor.this.uncaughtExceptionHandler);
            this.name = name;
            this.id = id;
        }

        void start() {
            LOGGER.info("Starting {} thread", this.name);
            this.thread.start();
        }

    }

    private static class LocalMonitorRunnable implements Runnable {

        private final ILocalMonitorService localMonitorService;

        LocalMonitorRunnable(ILocalMonitorService localMonitorService) {
            this.localMonitorService = localMonitorService;
        }

        @Override
        public void run() {
            try {
                localMonitorService.start();
            } catch (LocalMonitorException e) {
                throw new RuntimeException(e);
            }
        }

    }

}
