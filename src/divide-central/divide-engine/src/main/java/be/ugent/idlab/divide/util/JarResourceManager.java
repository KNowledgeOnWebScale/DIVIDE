package be.ugent.idlab.divide.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static be.ugent.idlab.divide.util.Constants.DIVIDE_DIRECTORY;

public class JarResourceManager {

    public static final int DEFAULT_BUFFER_SIZE = 8192;

    private static JarResourceManager instance;

    private JarResourceManager() {}

    public static JarResourceManager getInstance() {
        if (instance == null) {
            instance = new JarResourceManager();
        }
        return instance;
    }

    @SuppressWarnings("SameParameterValue")
    public synchronized InputStream getResourceFileAsStream(String fileName) {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(fileName);

        if (inputStream == null) {
            throw new IllegalArgumentException("Resource file " + fileName + " does not exist");
        } else {
            return inputStream;
        }
    }

    public synchronized String copyResourceToFile(Path resourceFilePath) throws IOException {
        return copyResourceToFile(resourceFilePath, null);
    }

    public synchronized String copyResourceToFile(Path resourceFilePath, String directory) throws IOException {
        InputStream inputStream = getResourceFileAsStream(resourceFilePath.toString());
        String resourceFileName = resourceFilePath.getFileName().toString();
        File file;
        if (directory == null) {
            file = new File(DIVIDE_DIRECTORY, resourceFileName);
            Files.createDirectories(Paths.get(DIVIDE_DIRECTORY));
        } else {
            Path destination = Paths.get(DIVIDE_DIRECTORY, directory);
            Files.createDirectories(destination);
            file = new File(destination.toString(), resourceFileName);
        }
        copyInputStreamToFile(inputStream, file);
        return file.getAbsolutePath();
    }

    private synchronized void copyInputStreamToFile(InputStream inputStream, File file) throws IOException {
        try (FileOutputStream outputStream = new FileOutputStream(file, false)) {
            int read;
            byte[] bytes = new byte[DEFAULT_BUFFER_SIZE];
            while ((read = inputStream.read(bytes)) != -1) {
                outputStream.write(bytes, 0, read);
            }
        }
    }

    public synchronized List<String> readLinesOfResourceFile(String fileName) throws IOException {
        try (final InputStream is = getResourceFileAsStream(fileName);
             final InputStreamReader isr =
                     new InputStreamReader(Objects.requireNonNull(is), StandardCharsets.UTF_8);
             final BufferedReader br = new BufferedReader(isr)) {
            return br.lines().collect(Collectors.toList());
        }
    }

    public synchronized String readResourceFile(String fileName) throws IOException {
        try (final InputStream is = getResourceFileAsStream(fileName);
             final InputStreamReader isr =
                     new InputStreamReader(Objects.requireNonNull(is), StandardCharsets.UTF_8);
             final BufferedReader br = new BufferedReader(isr)) {
            return br.lines().collect(Collectors.joining(System.getProperty("line.separator")));
        }
    }

}
