package be.ugent.idlab.divide.monitor.local.monitoring.device;

import be.ugent.idlab.divide.monitor.local.LocalMonitorConstants;
import be.ugent.idlab.divide.monitor.local.LocalMonitorException;
import be.ugent.idlab.divide.monitor.local.monitoring.MonitoringService;
import be.ugent.idlab.divide.monitor.local.util.JarResourceManager;
import be.ugent.idlab.rspservice.common.interfaces.RDFStream;
import be.ugent.idlab.rspservice.common.interfaces.RDFStreamProcessor;
import be.ugent.idlab.util.io.IOUtilities;
import be.ugent.idlab.util.process.ProcessException;
import be.ugent.idlab.util.process.ProcessUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Device monitor class which is a Java wrapper around the device monitoring Python script
 * called 'call-device-monitor.py'.
 */
class DeviceMonitor extends MonitoringService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeviceMonitor.class.getName());

    // name and start-up call for Python device monitoring script
    private static final String SCRIPT_EXECUTOR = "python3";
    private static final String SCRIPT_NAME = "call-device-monitor.py";

    private Process process;

    // actual location of script on local file system
    private String scriptPath;

    DeviceMonitor(RDFStreamProcessor rdfStreamProcessor,
                  RDFStream localMonitorStream,
                  String componentId,
                  String deviceId) {
        super(rdfStreamProcessor, localMonitorStream, componentId, deviceId);
    }

    @Override
    public void start() throws LocalMonitorException {
        // create an actual file on the file system from the script's resource file
        LOGGER.info("Copying device monitor script resource from JAR to dedicated file {}", SCRIPT_NAME);
        try {
            this.scriptPath = JarResourceManager.getInstance().copyResourceToFile(Paths.get(SCRIPT_NAME));
        } catch (IOException e) {
            throw new LocalMonitorException("Device monitor Python script could not be copied " +
                    "from the JAR resources to a dedicated file on the file system", e);
        }

        // schedule periodic execution of device monitor script
        LOGGER.info("Periodically schedule device monitor script for execution");
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        ScheduledFuture<?> handle =
                scheduler.scheduleAtFixedRate(this::callMonitor, 5,
                        LocalMonitorConstants.DEVICE_MONITOR_EXECUTION_PERIOD_IN_SECONDS, TimeUnit.SECONDS);

        // watch the periodic script execution to intercept any exceptions
        try {
            handle.get();
        } catch (ExecutionException e) {
            String message = "Error during execution of device monitor Python script";
            // not logged since LocalMonitor exception handler will report exception
            throw new LocalMonitorException(message, e);
        } catch (InterruptedException e) {
            String message = "Device monitor Python script interrupted while waiting between two runs";
            // not logged since LocalMonitor exception handler will report exception
            throw new LocalMonitorException(message, e);
        }
    }

    private void callMonitor() {
        // retrieve monitor output
        String monitorOutputJson = runProcess();
        // handle output
        handleMonitoringOutput(monitorOutputJson);
    }

    private String runProcess() {
        if (process == null || !process.isAlive()) {
            if (process != null) {
                process.destroyForcibly();
            }
            try {
                process = Runtime.getRuntime().exec(new String[]{SCRIPT_EXECUTOR, this.scriptPath});
                String result = IOUtilities.removeWhiteSpace(ProcessUtilities.handleProcess(process));
                LOGGER.info("Device monitor script output: {}", result);
                return result;
            } catch (IOException | ProcessException e) {
                throw new RuntimeException(e);
            }
        } else {
            return null;
        }
    }

    @Override
    public void reset() {
        if (process != null) {
            process.destroy();
        }
    }

}
