package be.ugent.idlab.divide.monitor;

import be.ugent.idlab.divide.core.component.IComponent;
import be.ugent.idlab.divide.core.engine.IDivideEngine;
import be.ugent.idlab.divide.core.exception.DivideNotInitializedException;
import be.ugent.idlab.divide.monitor.metamodel.IDivideMetaModel;
import be.ugent.idlab.divide.util.JarResourceManager;
import be.ugent.idlab.util.process.ProcessException;
import be.ugent.idlab.util.process.ProcessUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

class DivideMonitor implements IDivideMonitor {

    private static final Logger LOGGER = LoggerFactory.getLogger(DivideMonitor.class.getName());

    private final IDivideEngine divideEngine;
    private final IDivideGlobalMonitor globalMonitor;
    private final String localMonitorJarPath;
    private final String deviceNetworkIp;

    private final Map<String, LocalMonitorManager> localMonitorManagerMap;

    DivideMonitor(IDivideEngine divideEngine,
                  IDivideGlobalMonitor globalMonitor,
                  String localMonitorJarPath,
                  String deviceNetworkIp) {
        LOGGER.info("Creating a DIVIDE Monitor with initialized Global Monitor and " +
                "local monitor JAR path set to '{}'", localMonitorJarPath);

        this.divideEngine = divideEngine;
        this.globalMonitor = globalMonitor;
        this.localMonitorJarPath = localMonitorJarPath;
        this.deviceNetworkIp = deviceNetworkIp;

        this.localMonitorManagerMap = new HashMap<>();
    }

    @Override
    public IDivideMetaModel getDivideMetaModel() {
        return this.globalMonitor.getDivideMetaModel();
    }

    @Override
    public synchronized void start()
            throws DivideNotInitializedException, MonitorException {
        LOGGER.info("Starting the DIVIDE Monitor");

        // ensure that the Python requirements are installed to call
        // the manage_local_monitor.py script
        installPythonRequirements();

        // start global monitor
        this.globalMonitor.start();

        // manage local monitor: perform general initialization task &
        // start a local monitor on every registered component
        LocalMonitorManager.initialize();
        for (IComponent registeredComponent : divideEngine.getRegisteredComponents()) {
            ensureLocalMonitorIsActive(registeredComponent);
        }
    }

    private void installPythonRequirements() throws MonitorException {
        try {
            LOGGER.info("Installing Python requirements for Local Monitor");
            String requirementsPath = JarResourceManager.getInstance().copyResourceToFile(
                    Paths.get("monitor", "python-requirements.txt"), "monitor");
            String output = ProcessUtilities.executeProcess(new String[]{
                    "python3", "-m", "pip", "install", "-r", requirementsPath});
            LOGGER.info("Output of installing Python requirements: {}", output);
        } catch (ProcessException | IOException e) {
            throw new MonitorException("Could not install Python requirements for " +
                    "Local Monitor manager", e);
        }
    }

    @Override
    public synchronized void addComponent(IComponent component) throws MonitorException {
        ensureLocalMonitorIsActive(component);
    }

    @Override
    public synchronized void removeComponent(IComponent component) throws MonitorException {
        if (this.localMonitorManagerMap.containsKey(component.getId())) {
            LOGGER.info("Removing & stopping the Local Monitor on removed " +
                            "component with ID {}", component.getId());

            LocalMonitorManager localMonitorManager =
                    this.localMonitorManagerMap.get(component.getId());
            this.localMonitorManagerMap.remove(component.getId());
            if (localMonitorManager.getStatus() == LocalMonitorStatus.RUNNING) {
                LOGGER.info("Local Monitor on removed component with ID {} is RUNNING -> stopping",
                        component.getId());
                localMonitorManager.stop();
            } else {
                LOGGER.info("Local Monitor on removed component with ID {} is NOT RUNNING -> NOT stopping",
                        component.getId());
            }

        } else {
            LOGGER.info("No Local Monitor deployed on removed component with ID {}",
                    component.getId());
        }
    }

    private void ensureLocalMonitorIsActive(IComponent component) throws MonitorException {
        LOGGER.info("Ensuring a Local Monitor is active on registered component with ID {}",
                component.getId());

        if (!this.localMonitorManagerMap.containsKey(component.getId())) {
            LocalMonitorManager localMonitorManager =
                    new LocalMonitorManager(component, localMonitorJarPath, deviceNetworkIp);
            this.localMonitorManagerMap.put(component.getId(), localMonitorManager);
            localMonitorManager.start();

        } else {
            LocalMonitorManager localMonitorManager =
                    this.localMonitorManagerMap.get(component.getId());
            if (localMonitorManager.getStatus() != LocalMonitorStatus.RUNNING) {
                LOGGER.info("A Local Monitor is already deployed on registered " +
                                "component with ID {}, but it is currently not running",
                        component.getId());
                localMonitorManager.start();
            } else {
                LOGGER.info("A Local Monitor is already deployed on registered " +
                                "component with ID {}, and is currently running",
                        component.getId());
            }
        }
    }

}
