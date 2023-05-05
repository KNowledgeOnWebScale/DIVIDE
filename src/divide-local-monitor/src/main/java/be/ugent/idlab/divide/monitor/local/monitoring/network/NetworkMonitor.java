package be.ugent.idlab.divide.monitor.local.monitoring.network;

import be.ugent.idlab.divide.monitor.local.LocalMonitorConstants;
import be.ugent.idlab.divide.monitor.local.LocalMonitorException;
import be.ugent.idlab.divide.monitor.local.monitoring.MonitoringService;
import be.ugent.idlab.divide.monitor.local.util.JarResourceManager;
import be.ugent.idlab.rspservice.common.interfaces.RDFStream;
import be.ugent.idlab.rspservice.common.interfaces.RDFStreamProcessor;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Paths;

/**
 * Network monitor class which is a Java wrapper around the network monitoring Python script
 * called 'network-monitor.py'.
 */
class NetworkMonitor extends MonitoringService {

    private static final Logger LOGGER = LoggerFactory.getLogger(NetworkMonitor.class.getName());

    // name and start-up call for Python network monitoring script
    private static final String[] SCRIPT_EXECUTOR = new String[]{"python3", "-u"};
    private static final String SCRIPT_NAME = "network-monitor.py";

    private Process process;
    private final String publicNetworkInterface;
    private final String centralHost;

    // actual location of script on local file system
    private String scriptPath;

    NetworkMonitor(RDFStreamProcessor rdfStreamProcessor,
                   RDFStream localMonitorStream,
                   String componentId,
                   String deviceId,
                   String publicNetworkInterface,
                   String centralHost) {
        super(rdfStreamProcessor, localMonitorStream, componentId, deviceId);
        this.publicNetworkInterface = publicNetworkInterface;
        this.centralHost = centralHost;
    }

    @Override
    public void start() throws LocalMonitorException {
        // create an actual file on the file system from the script's resource file
        LOGGER.info("Copying network monitor script resource from JAR to dedicated file {}", SCRIPT_NAME);
        try {
            this.scriptPath = JarResourceManager.getInstance().copyResourceToFile(Paths.get(SCRIPT_NAME));
        } catch (IOException e) {
            throw new LocalMonitorException("Network monitor Python script could not be copied " +
                    "from the JAR resources to a dedicated file on the file system", e);
        }

        // schedule periodic execution of network monitor script
        LOGGER.info("Start the network monitor script (which will continuously run)");
        callMonitor();
    }

    private void callMonitor() throws LocalMonitorException {
        // start process
        runProcess();
    }

    private void runProcess() throws LocalMonitorException {
        if (process == null || !process.isAlive()) {
            if (process != null) {
                process.destroyForcibly();
            }
            try {
                // call monitor script
                String[] processCall = ArrayUtils.addAll(
                        SCRIPT_EXECUTOR, this.scriptPath, this.centralHost, this.publicNetworkInterface,
                        Integer.toString(LocalMonitorConstants.NETWORK_MONITOR_PING_PERIOD_IN_SECONDS),
                        Integer.toString(LocalMonitorConstants.NETWORK_MONITOR_EXECUTION_PERIOD_IN_SECONDS));
                LOGGER.info("Starting the following process: {}", String.join(" ", processCall));
                process = Runtime.getRuntime().exec(processCall);

                // create reader for process output
                InputStream inputStream = process.getInputStream();
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

                // process every output line
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    LOGGER.info("Network monitor script output: {}", line);
                    handleMonitoringOutput(line);
                }

                // wait for process to finish
                process.waitFor();

            } catch (IOException | InterruptedException e) {
                String message = "Network monitor Python script failed or interrupted";
                // not logged since LocalMonitor exception handler will report exception
                throw new LocalMonitorException(message, e);
            }
        }
    }

    @Override
    public void reset() {
        if (process != null) {
            process.destroy();
        }
    }

}
