package be.ugent.idlab.divide.monitor.local.monitoring.device;

import be.ugent.idlab.divide.monitor.local.ILocalMonitorService;
import be.ugent.idlab.divide.monitor.local.LocalMonitorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Device monitor class which is a Java wrapper around the device monitoring Python script
 * called 'start-device-monitor.py'.
 */
class DeviceMonitor_Old implements ILocalMonitorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeviceMonitor_Old.class.getName());

    // name and start-up call for Python device monitoring script
    private static final String SCRIPT_NAME = "call-device-monitor.py";
    private static final String SCRIPT_STARTUP = String.format("python3 %s", SCRIPT_NAME);

    private Process process;

    private String localMonitorStreamUrl;

    public static void main(String[] args) throws LocalMonitorException {
        DeviceMonitor_Old x = new DeviceMonitor_Old();
        x.start();
    }

    DeviceMonitor_Old() {
        /*String endpoint = localMonitorConfig.getEndpoint();
        if (endpoint.startsWith("/")) {
            endpoint = endpoint.substring(1);
        }
        this.localMonitorStreamUrl = String.format("http://localhost:%d/%s",
                localMonitorConfig.getPort(), endpoint);*/
    }

    /**
     * The default buffer size
     */
    public static final int DEFAULT_BUFFER_SIZE = 8192;

    @Override
    public void start() throws LocalMonitorException {
        // obtain
        InputStream inputStream = getResourceFileAsStream(SCRIPT_NAME);
        /*String content = new BufferedReader(new InputStreamReader(inputStream))
                .lines().collect(Collectors.joining("\n"));
        System.out.println(content);*/

        // create an actual file on the file system from the script's resource file
        File file = new File(SCRIPT_NAME);
        try {
            copyInputStreamToFile(inputStream, file);
        } catch (IOException e) {
            e.printStackTrace();
        }

        /*try {
            ProcessBuilder pb = new ProcessBuilder("./testing.sh");
            Process p = pb.start();
            BufferedReader reader =
                    new BufferedReader(new InputStreamReader(p.getInputStream()));
            StringBuilder builder = new StringBuilder();
            String line;
            while ( (line = reader.readLine()) != null) {
                builder.append(line);
                System.out.println(line);
                builder.append(System.getProperty("line.separator"));
            }
            String result = builder.toString();
            System.out.println("[" + result + "]");

            process = Runtime.getRuntime().exec(SCRIPT_STARTUP);
        } catch (IOException e) {
            throw new DeviceMonitorException("Failed to start the DIVIDE Device Monitor", e);
        }*/

        /*System.out.println("here");
        System.out.println(process);
        System.out.println(process.isAlive());*/

        Timer t = new Timer();
        TimerTask mTask = new TimerTask() {
            @Override
            public void run() {
                if (process == null || !process.isAlive()) {
                    if (process != null) {
                        process.destroyForcibly();
                    }
                    try {
                        process = Runtime.getRuntime().exec(SCRIPT_STARTUP);

                        BufferedReader reader =
                                new BufferedReader(new InputStreamReader(process.getInputStream()));
                        StringBuilder builder = new StringBuilder();
                        String line;
                        while ( (line = reader.readLine()) != null) {
                            builder.append(line);
                            builder.append(System.getProperty("line.separator"));
                        }
                        String result = builder.toString();
                        System.out.println(result);

                    } catch (IOException ignored) {
                    }
                }
            }
        };
        // This task is scheduled to run every 10 seconds
        t.scheduleAtFixedRate(mTask, 0, 10000);

        System.out.println("there");
    }

    @Override
    public void reset() {
        if (process != null) {
            process.destroy();
        }
    }

    @SuppressWarnings("SameParameterValue")
    private InputStream getResourceFileAsStream(String fileName) {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(fileName);

        if (inputStream == null) {
            throw new IllegalArgumentException("Resource file " + fileName + " does not exist");
        } else {
            return inputStream;
        }
    }

    private static void copyInputStreamToFile(InputStream inputStream, File file) throws IOException {
        try (FileOutputStream outputStream = new FileOutputStream(file, false)) {
            int read;
            byte[] bytes = new byte[DEFAULT_BUFFER_SIZE];
            while ((read = inputStream.read(bytes)) != -1) {
                outputStream.write(bytes, 0, read);
            }
        }
    }

}
