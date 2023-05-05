package be.ugent.idlab.divide.monitor;

import be.ugent.idlab.divide.core.component.IComponent;
import be.ugent.idlab.divide.util.JarResourceManager;
import be.ugent.idlab.util.process.ProcessException;
import be.ugent.idlab.util.process.ProcessUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Paths;

class LocalMonitorManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(LocalMonitorManager.class.getName());

    // name and start-up call for Python local monitor manager script
    private static final String SCRIPT_EXECUTOR = "python3";
    private static final String SCRIPT_NAME = "monitor/manage_local_monitor.py";
    private static String SCRIPT_PATH_ON_FILE_SYSTEM;

    private final IComponent component;
    private final String localMonitorJarPath;
    private final String centralIpAddress;

    private boolean firstStarted;

    LocalMonitorManager(IComponent component,
                        String localMonitorJarPath,
                        String centralIpAddress) {
        this.component = component;
        this.localMonitorJarPath = localMonitorJarPath;
        this.centralIpAddress = centralIpAddress;

        // set the initial state to not started yet
        this.firstStarted = false;
    }

    static void initialize() throws MonitorException {
        // create an actual file on the file system from the script's resource file
        LOGGER.info("Copying local monitor manager Python script resource" +
                " from JAR to dedicated file {}", SCRIPT_NAME);
        try {
            SCRIPT_PATH_ON_FILE_SYSTEM = JarResourceManager.getInstance().copyResourceToFile(
                    Paths.get(SCRIPT_NAME), "monitor");
        } catch (IOException e) {
            throw new MonitorException("Local monitor manager Python script could not be copied " +
                    "from the JAR resources to a dedicated file on the file system", e);
        }
    }

    void start() throws MonitorException {
        try {
            LOGGER.info("Starting Local Monitor on component with ID {}", component.getId());
            this.firstStarted = true;
            String response = executeGlobalMonitorPythonManager("start");
            LOGGER.info("Output of script starting Local Monitor on component with ID {}: {}",
                    component.getId(), response);

        } catch (IOException | ProcessException e) {
            String message = String.format(
                    "Error while starting local monitor for component with ID %s",
                    component.getId());
            LOGGER.error(message, e);
            throw new MonitorException(message, e);
        }
    }

    void stop() throws MonitorException {
        try {
            LOGGER.info("Stopping Local Monitor on component with ID {}", component.getId());
            String response = executeGlobalMonitorPythonManager("stop");
            LOGGER.info("Output of script stopping Local Monitor on component with ID {}: {}",
                    component.getId(), response);

        } catch (IOException | ProcessException e) {
            String message = String.format(
                    "Error while stopping local monitor for component with ID %s",
                    component.getId());
            LOGGER.error(message, e);
            throw new MonitorException(message, e);
        }
    }

    LocalMonitorStatus getStatus() throws MonitorException {
        LOGGER.info("Retrieving status of Local Monitor on component with ID {}",
                component.getId());

        // if the monitor has never started yet, the status is stopped by default
        if (!firstStarted) {
            LOGGER.info("Local Monitor on component with ID {} has never started yet, " +
                            "so status is 'STOPPED' by default", component.getId());
            return LocalMonitorStatus.STOPPED;
        }

        // otherwise, check the status on the local component via SSH
        try {
            String response = executeGlobalMonitorPythonManager("check");

            if (response.isEmpty()) {
                throw new MonitorException(String.format("Status check of local monitor with ID %s returned an " +
                        "empty response", component.getId()));
            }

            LOGGER.info("Output of status check of Local Monitor on component with ID {}: {}",
                    component.getId(), response);
            if (response.contains("status:running")) {
                return LocalMonitorStatus.RUNNING;
            } else if (response.contains("status:stopped")) {
                return LocalMonitorStatus.STOPPED;
            } else { // default
                return LocalMonitorStatus.STOPPED;
            }

        } catch (IOException | ProcessException e) {
            String message = String.format(
                    "Error while checking status of local monitor for component with ID %s",
                    component.getId());
            LOGGER.error(message, e);
            throw new MonitorException(message, e);
        }
    }

    private String executeGlobalMonitorPythonManager(String action) throws ProcessException, IOException {
        return ProcessUtilities.executeProcess(new String[]{
                SCRIPT_EXECUTOR, SCRIPT_PATH_ON_FILE_SYSTEM,
                "--component-id", component.getId(),
                "--component-ip-address", component.getIpAddress(),
                "--monitor-jar-path", localMonitorJarPath,
                "--action", action,
                "--global-monitor-reasoning-service-port", String.valueOf(
                        DivideGlobalMonitorConstants.RS_SERVER_PORT),
                "--global-monitor-reasoning-service-uri", DivideGlobalMonitorConstants.RS_SERVER_URI,
                "--divide-central-ip-address", centralIpAddress
        });
    }

}
