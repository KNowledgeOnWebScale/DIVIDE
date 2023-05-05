package be.ugent.idlab.divide.util.component;

import be.ugent.idlab.divide.core.engine.IDivideEngine;
import be.ugent.idlab.divide.rsp.RspQueryLanguage;
import be.ugent.idlab.util.io.IOUtilities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Class used for parsing configuration of component entries.
 * These configurations can be present in a CSV file (e.g. used during start-up
 * of a DIVIDE server with a known set of components) or in the HTTP body of
 * a component creation request to the DIVIDE API.
 */
public class CsvComponentEntryParser {

    private static final String DELIMITER = ";";

    /**
     * Parses a CSV file containing DIVIDE component entries on each line.
     * A single line uses the delimiter ';' to split the different elements
     * of the configuration entry.
     *
     * @param csvFile path to CSV file containing component configurations
     * @return a list of parsed component entries of which the gettable fields can
     *         directly be used as input for the registration of components in a
     *         DIVIDE engine using the {@link IDivideEngine#registerComponent(
     *         String, List, RspQueryLanguage, int)} method
     * @throws ComponentEntryParserException if a component configuration in the CSV file is
     *                                       invalid (invalid list of additional context IRIs,
     *                                       invalid RSP engine URL, or invalid
     *                                       RSP query language)
     * @throws IllegalArgumentException if CSV file does not exist or is empty
     */
    public static List<ComponentEntry> parseComponentEntryFile(String csvFile)
            throws ComponentEntryParserException {
        List<ComponentEntry> componentEntries = new ArrayList<>();

        // read CSV file
        List<String[]> componentEntryStrings = IOUtilities.readCsvFile(csvFile, DELIMITER);
        if (componentEntryStrings.isEmpty()) {
            throw new IllegalArgumentException("CSV file does not exist or is empty");
        }

        // parse component entries in CSV file
        for (String[] componentEntryString : componentEntryStrings) {
            componentEntries.add(parseComponentEntry(componentEntryString));
        }

        return componentEntries;
    }

    private static ComponentEntry parseComponentEntry(String[] entry)
            throws ComponentEntryParserException {
        if (entry.length == 5) {
            // retrieve IP address (or host name)
            String ipAddress = entry[0];

            // retrieve main context IRI
            String mainContextIri = entry[1].trim();

            // convert array string to actual array of additional context IRIs
            if (!entry[2].trim().matches("\\[[^\\[\\]]*]")) {
                throw new ComponentEntryParserException(
                        "Component entry contains invalid list of additional IRIs");
            }
            List<String> contextIris = new ArrayList<>();
            contextIris.add(mainContextIri);
            if (!entry[2].replace(" ", "").replace("\t", "").trim().equals("[]")) {
                contextIris.addAll(
                        Arrays.stream(entry[2].replace("[", "").replace("]", "").split(","))
                                .map(String::trim)
                                .collect(Collectors.toList()));
            }
            ComponentEntryParser.validateContextIris(contextIris);

            // parse RSP query language
            RspQueryLanguage rspQueryLanguage =
                    ComponentEntryParser.parseRspEngineQueryLanguage(entry[3]);

            // parse RSP engine URL
            int rspEngineServerPort = Integer.parseInt(entry[4].trim());

            // if no errors, then return new component entry
            return new ComponentEntry(
                    ipAddress, contextIris, rspQueryLanguage, rspEngineServerPort);

        } else {
            throw new ComponentEntryParserException(
                    "Component entry does not contain 5 elements");
        }
    }

}
