package be.ugent.idlab.divide.queryderivation.eye;

import be.ugent.idlab.divide.core.context.Context;
import be.ugent.idlab.divide.core.context.ContextEnrichingQuery;
import be.ugent.idlab.divide.core.engine.IDivideQueryDeriver;
import be.ugent.idlab.divide.core.engine.IDivideQueryDeriverResult;
import be.ugent.idlab.divide.core.exception.DivideInitializationException;
import be.ugent.idlab.divide.core.exception.DivideInvalidInputException;
import be.ugent.idlab.divide.core.exception.DivideNotInitializedException;
import be.ugent.idlab.divide.core.exception.DivideQueryDeriverException;
import be.ugent.idlab.divide.core.query.IDivideQuery;
import be.ugent.idlab.divide.core.query.parser.IDivideQueryParser;
import be.ugent.idlab.divide.core.query.parser.InvalidDivideQueryParserInputException;
import be.ugent.idlab.divide.core.query.parser.ParsedSparqlQuery;
import be.ugent.idlab.divide.core.query.parser.Prefix;
import be.ugent.idlab.divide.core.query.parser.SplitSparqlQuery;
import be.ugent.idlab.divide.util.LogConstants;
import be.ugent.idlab.util.bash.BashException;
import be.ugent.idlab.util.eye.EyeReasoner;
import be.ugent.idlab.util.io.IOUtilities;
import be.ugent.idlab.util.rdf.RDFLanguage;
import be.ugent.idlab.util.rdf.jena3.owlapi4.JenaUtilities;
import org.apache.commons.io.FileUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.update.UpdateAction;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

@SuppressWarnings({"FieldCanBeLocal", "ResultOfMethodCallIgnored"})
class EyeDivideQueryDeriver implements IDivideQueryDeriver {

    private static final Logger LOGGER = LoggerFactory.getLogger(EyeDivideQueryDeriver.class.getName());

    /**
     * Hidden directory where EYE stores all its files related to the
     * DIVIDE query derivation
     */
    private static final String DIVIDE_DIRECTORY = ".divide";

    /**
     * Path of JAR resource for generating all triples, used during the EYE preprocessing
     */
    private static final String PREPROCESSING_INSTANTIATE_TRIPLES_RESOURCE =
            Paths.get("eye", "n3", "preprocessing", "instantiate-triples.n3").toString();

    /**
     * Path of JAR resource for instantiating rules, used during the EYE preprocessing
     */
    private static final String PREPROCESSING_INSTANTIATE_RULES_RESOURCE =
            Paths.get("eye", "n3", "preprocessing", "instantiate-rules.n3").toString();

    /**
     * Path of JAR resource for processing lists, used during the EYE preprocessing
     */
    private static final String PREPROCESSING_LISTS_RESOURCE =
            Paths.get("eye", "n3", "preprocessing", "lists.n3").toString();

    /**
     * Path of JAR resource describing the query extraction goal, used during the
     * EYE query derivation
     */
    private static final String QUERY_EXTRACTION_GOAL_RESOURCE =
            Paths.get("eye", "n3", "query-derivation", "query-extraction-goal.n3").toString();

    /**
     * Path of JAR resource describing the window parameter extraction goal, used during the
     * EYE query derivation
     */
    private static final String WINDOW_PARAMETER_EXTRACTION_GOAL_RESOURCE =
            Paths.get("eye", "n3", "query-derivation", "window-parameter-extraction-goal.n3").toString();

    /**
     * Path of JAR resource describing the goal for substituting the input variables
     * into the query body, used during the EYE query derivation
     */
    private static final String QUERY_INPUT_VARIABLE_SUBSTITUTION_GOAL_RESOURCE =
            Paths.get("eye", "n3", "query-derivation",
                    "query-input-variable-substitution-goal.n3").toString();

    /**
     * Path of JAR resource describing rules for substituting the input variables
     * into the query body, used during the EYE query derivation
     */
    private static final String QUERY_INPUT_VARIABLE_SUBSTITUTION_RULES_RESOURCE =
            Paths.get("eye", "n3", "query-derivation",
                    "query-input-variable-substitution-rules.n3").toString();

    /**
     * Path of JAR resource describing the supported datatypes for substituting the
     * input variables into the query body, used during the EYE query derivation
     */
    private static final String QUERY_INPUT_VARIABLE_SUBSTITUTION_SUPPORTED_DATATYPES_RESOURCE =
            Paths.get("eye", "n3", "query-derivation",
                    "query-input-variable-substitution-supported-datatypes.n3").toString();

    /**
     * Path of JAR resource describing the goal for substituting the dynamic window
     * parameters into the query body, used during the EYE query derivation
     */
    private static final String QUERY_DYNAMIC_WINDOW_PARAMETER_SUBSTITUTION_GOAL_RESOURCE =
            Paths.get("eye", "n3", "query-derivation",
                    "query-dynamic-window-parameter-substitution-goal.n3").toString();

    /**
     * Path of JAR resource describing the rules for substituting the dynamic window
     * parameters into the query body, used during the EYE query derivation
     */
    private static final String QUERY_DYNAMIC_WINDOW_PARAMETER_SUBSTITUTION_RULES_RESOURCE =
            Paths.get("eye", "n3", "query-derivation",
                    "query-dynamic-window-parameter-substitution-rules.n3").toString();

    /**
     * Path of JAR resource describing the goal for substituting the static window
     * parameters into the query body, used during the EYE query derivation
     */
    private static final String QUERY_STATIC_WINDOW_PARAMETER_SUBSTITUTION_GOAL_RESOURCE =
            Paths.get("eye", "n3", "query-derivation",
                    "query-static-window-parameter-substitution-goal.n3").toString();

    /**
     * Path of JAR resource describing the rules for substituting the static window
     * parameters into the query body, used during the EYE query derivation
     */
    private static final String QUERY_STATIC_WINDOW_PARAMETER_SUBSTITUTION_RULES_RESOURCE =
            Paths.get("eye", "n3", "query-derivation",
                    "query-static-window-parameter-substitution-rules.n3").toString();

    /**
     * Path of JAR resource describing a context change as trigger for the
     * EYE query derivation
     */
    private static final String QUERY_DERIVATION_TRIGGER_CONTEXT_CHANGE_RESOURCE =
            Paths.get("eye", "n3", "query-derivation", "trigger",
                    "trigger-context-change.n3").toString();

    /**
     * Path of JAR resource describing the monitor as trigger for the
     * EYE query derivation
     */
    private static final String QUERY_DERIVATION_TRIGGER_MONITOR_RESOURCE =
            Paths.get("eye", "n3", "query-derivation", "trigger",
                    "trigger-monitor.n3").toString();

    /**
     * Template of path of query pattern file for a DIVIDE query
     * (still to be instantiated with concrete query name)
     */
    private static final String EYE_DIVIDE_QUERY_QUERY_PATTERN_PATH_TEMPLATE =
            Paths.get("eye", "queries", "%s", "query-pattern.n3").toString();

    /**
     * Template of path of sensor query rule file for a DIVIDE query
     * (still to be instantiated with concrete query name)
     */
    private static final String EYE_DIVIDE_QUERY_SENSOR_QUERY_RULE_PATH_TEMPLATE =
            Paths.get("eye", "queries", "%s", "sensor-query-rule.n3").toString();

    /**
     * Template of path of goal file for a DIVIDE query
     * (still to be instantiated with concrete query name)
     */
    private static final String EYE_DIVIDE_QUERY_GOAL_PATH_TEMPLATE =
            Paths.get("eye", "queries", "%s", "goal.n3").toString();

    /**
     * Template of path of file for a context-enriching query of a DIVIDE query
     * written as an EYE rule (still to be instantiated with concrete query name
     * and context-enriching query number)
     */
    private static final String EYE_DIVIDE_QUERY_CONTEXT_ENRICHING_QUERY_PATH_TEMPLATE =
            Paths.get("eye", "queries", "%s", "context-enriching-query-rule-%d.n3").toString();

    /**
     * Date formatter used to create directories to store the timestamped
     * results of the EYE query derivation
     */
    private static final SimpleDateFormat formatter =
            new SimpleDateFormat("yyyyMMdd_HHmmss");

    /**
     * Boolean representing whether the ontology has already been successfully loaded
     */
    private boolean ontologyLoaded;

    // DIVIDE files
    private final String preprocessingInstantiateTriplesFile;
    private final String preprocessingInstantiateRulesFile;
    private final String preprocessingListsFile;
    private final String queryExtractionGoalFile;
    private final String windowParameterExtractionGoalFile;
    private final String queryInputVariableSubstitutionGoalFile;
    private final String queryInputVariableSubstitutionRulesFile;
    private final String queryInputVariableSubstitutionSupportedDatatypesFile;
    private final String queryDynamicWindowParameterSubstitutionGoalFile;
    private final String queryDynamicWindowParameterSubstitutionRulesFile;
    private final String queryStaticWindowParameterSubstitutionGoalFile;
    private final String queryStaticWindowParameterSubstitutionRulesFile;
    private final String queryDerivationTriggerContextChangeFile;
    private final String queryDerivationTriggerMonitorFile;

    // preprocessing input files & options
    private final List<String> preprocessingOntologyCreationOptions;
    private final List<String> preprocessingTripleCreationInputFiles;
    private final List<String> preprocessingTripleCreationOptions;
    private final List<String> preprocessingRuleCreationInputFiles;
    private final List<String> preprocessingRuleCreationOptions;
    private final List<String> preprocessingImageCreationInputFiles;

    // preprocessing output files
    private final String ontologyFile;
    private final String triplesFile;
    private final String rulesFile;
    private final String imageFile;
    private final String imageFileLoading;

    // query derivation bash inputs
    private final List<String> queryExtractionOptions;
    private final List<String> windowParameterExtractionOptions;
    private final List<String> querySubstitutionOptions;

    /**
     * Map keeping track of link between query name and specific
     * DIVIDE EYE query
     */
    private final Map<String, EyeDivideQuery> divideQueryMap;

    /**
     * Map keeping track of link between a prefix URI and its RSP-QL prefix
     * string converted by an instance of {@link EyeDivideQueryDeriver}
     */
    private final Map<String, String> convertedPrefixesMap;

    private final boolean handleTBoxDefinitionsInContext;


    // INPUTS FOR SPARQL QUERIES
    private static final String SPARQL_QUERY_PREPARE_CONTEXT_INITIAL_RESOURCE =
            Paths.get("sparql", "prepare-context-for-query-derivation-initial.query").toString();
    private static final String SPARQL_QUERY_PREPARE_CONTEXT_LOOP_RESOURCE =
            Paths.get("sparql", "prepare-context-for-query-derivation-loop.query").toString();
    private final UpdateRequest sparqlQueryPrepareContextInitial;
    private final UpdateRequest sparqlQueryPrepareContextLoop;


    // INPUT CONTAINING TRIPLE SPECIFYING SUBSTITUTION TRIGGER
    private enum SubstitutionTrigger {
        CONTEXT_CHANGE, MONITOR
    }
    private final Map<SubstitutionTrigger, String> substitutionTriggerFilePathMap;


    EyeDivideQueryDeriver(boolean handleTBoxDefinitionsInContext) throws DivideQueryDeriverException {
        try {
            // set ontology loaded flag to false
            this.ontologyLoaded = false;

            // initialize query map
            this.divideQueryMap = new HashMap<>();

            // initialize map to keep track of prefixes converted by EyeDivideQueryConverter
            this.convertedPrefixesMap = new HashMap<>();

            // save boolean about handling TBox definitions in context
            this.handleTBoxDefinitionsInContext = handleTBoxDefinitionsInContext;

            // create DIVIDE directory
            File divideDirectory = new File(DIVIDE_DIRECTORY);
            boolean created = divideDirectory.mkdir();
            if (!created) {
                // if directory already exists, remove all files
                FileUtils.cleanDirectory(divideDirectory);
            }

            // create copies of resource files in DIVIDE directory
            preprocessingInstantiateTriplesFile =
                    copyResourceToDivideDirectory(PREPROCESSING_INSTANTIATE_TRIPLES_RESOURCE);
            preprocessingInstantiateRulesFile =
                    copyResourceToDivideDirectory(PREPROCESSING_INSTANTIATE_RULES_RESOURCE);
            preprocessingListsFile =
                    copyResourceToDivideDirectory(PREPROCESSING_LISTS_RESOURCE);
            queryExtractionGoalFile =
                    copyResourceToDivideDirectory(QUERY_EXTRACTION_GOAL_RESOURCE);
            windowParameterExtractionGoalFile =
                    copyResourceToDivideDirectory(WINDOW_PARAMETER_EXTRACTION_GOAL_RESOURCE);
            queryInputVariableSubstitutionGoalFile =
                    copyResourceToDivideDirectory(QUERY_INPUT_VARIABLE_SUBSTITUTION_GOAL_RESOURCE);
            queryInputVariableSubstitutionRulesFile =
                    copyResourceToDivideDirectory(QUERY_INPUT_VARIABLE_SUBSTITUTION_RULES_RESOURCE);
            queryInputVariableSubstitutionSupportedDatatypesFile =
                    copyResourceToDivideDirectory(QUERY_INPUT_VARIABLE_SUBSTITUTION_SUPPORTED_DATATYPES_RESOURCE);
            queryDynamicWindowParameterSubstitutionGoalFile =
                    copyResourceToDivideDirectory(QUERY_DYNAMIC_WINDOW_PARAMETER_SUBSTITUTION_GOAL_RESOURCE);
            queryDynamicWindowParameterSubstitutionRulesFile =
                    copyResourceToDivideDirectory(QUERY_DYNAMIC_WINDOW_PARAMETER_SUBSTITUTION_RULES_RESOURCE);
            queryStaticWindowParameterSubstitutionGoalFile =
                    copyResourceToDivideDirectory(QUERY_STATIC_WINDOW_PARAMETER_SUBSTITUTION_GOAL_RESOURCE);
            queryStaticWindowParameterSubstitutionRulesFile =
                    copyResourceToDivideDirectory(QUERY_STATIC_WINDOW_PARAMETER_SUBSTITUTION_RULES_RESOURCE);
            queryDerivationTriggerContextChangeFile =
                    copyResourceToDivideDirectory(QUERY_DERIVATION_TRIGGER_CONTEXT_CHANGE_RESOURCE);
            queryDerivationTriggerMonitorFile =
                    copyResourceToDivideDirectory(QUERY_DERIVATION_TRIGGER_MONITOR_RESOURCE);

            // set paths of ontology file, rules file & EYE image file
            // (which do not exist yet, but will be the output of the preprocessing)
            ontologyFile = Paths.get(DIVIDE_DIRECTORY, "eye", "n3", "ontology.n3").
                    toFile().getCanonicalPath();
            triplesFile = Paths.get(DIVIDE_DIRECTORY, "eye", "n3", "triples.n3").
                    toFile().getCanonicalPath();
            rulesFile = Paths.get(DIVIDE_DIRECTORY, "eye", "n3", "rules.n3").
                    toFile().getCanonicalPath();
            imageFile = Paths.get(DIVIDE_DIRECTORY, "eye", "ype.pvm").
                    toFile().getCanonicalPath();
            imageFileLoading = Paths.get(DIVIDE_DIRECTORY, "eye", "ype-loading.pvm").
                    toFile().getCanonicalPath();

            // set static inputs & options for the different steps of the ontology
            // preprocessing (to be readily available when preprocessing should
            // start, i.e., when the loadOntology method is called)
            preprocessingOntologyCreationOptions =
                    Arrays.asList("--no-qvars", "--nope");
            preprocessingTripleCreationInputFiles =
                    Arrays.asList(ontologyFile, preprocessingListsFile, preprocessingInstantiateTriplesFile);
            preprocessingTripleCreationOptions =
                    Arrays.asList("--nope", "--no-skolem",
                            "http://eulersharp.sourceforge.net/.well-known/genid/myVariables");
            preprocessingRuleCreationInputFiles =
                    Collections.singletonList(triplesFile);
            preprocessingRuleCreationOptions =
                    Arrays.asList("--nope", "--no-skolem",
                            "http://eulersharp.sourceforge.net/.well-known/genid/myVariables");
            preprocessingImageCreationInputFiles =
                    Arrays.asList(triplesFile, rulesFile);

            // set static inputs & options for query derivation
            // (to be readily available each time the query derivation is triggered,
            //  i.e., the deriveQueries method is called)
            queryExtractionOptions = Arrays.asList("--nope", "--tactic", "existing-path");
            windowParameterExtractionOptions = Collections.singletonList("--nope");
            querySubstitutionOptions = Collections.singletonList("--nope");

            // read queries to prepare context for query derivation,
            // and parse them as a JENA update query
            sparqlQueryPrepareContextInitial = UpdateFactory.create(
                    readResource(SPARQL_QUERY_PREPARE_CONTEXT_INITIAL_RESOURCE));
            sparqlQueryPrepareContextLoop = UpdateFactory.create(
                    readResource(SPARQL_QUERY_PREPARE_CONTEXT_LOOP_RESOURCE));

            // load substitution trigger map
            substitutionTriggerFilePathMap = new HashMap<>();
            substitutionTriggerFilePathMap.put(SubstitutionTrigger.CONTEXT_CHANGE,
                    queryDerivationTriggerContextChangeFile);
            substitutionTriggerFilePathMap.put(SubstitutionTrigger.MONITOR,
                    queryDerivationTriggerMonitorFile);

        } catch (IOException e) {
            throw new DivideQueryDeriverException(e);
        }
    }

    @Override
    public void loadOntology(Model ontology)
            throws DivideInvalidInputException, DivideInitializationException {
        LOGGER.info("LOADING ONTOLOGY: running the ontology preprocessing script " +
                "with the given ontology files as input");

        try {
            // write ontology to temp file
            String ontologyTurtleFile = writeToTempTurtleFile(ontology);

            // load ontology with EYE and write to N3 ontology file
            EyeReasoner.runToFile(
                    Collections.singletonList(ontologyTurtleFile),
                    ontologyFile,
                    preprocessingOntologyCreationOptions);

            // generate all triples by applying OWL-RL rules on N3 ontology and
            // write to N3 triples file
            EyeReasoner.runToFile(
                    preprocessingTripleCreationInputFiles,
                    triplesFile,
                    preprocessingTripleCreationOptions);

            // generate instantiated OWL-RL rules from collection of inferred
            // triples and write to N3 rules file
            EyeReasoner.runToFile(
                    preprocessingRuleCreationInputFiles,
                    preprocessingInstantiateRulesFile,
                    rulesFile,
                    preprocessingRuleCreationOptions);

            // create image of EYE reasoner that has the N3 ontology and rules files
            // preloaded into it
            // = intermediate code file resulting from Prolog compilation
            EyeReasoner.runToImage(preprocessingImageCreationInputFiles, imageFileLoading);

            // if everything is loaded successfully, the existing used image file is overwritten
            Files.copy(Paths.get(imageFileLoading), Paths.get(imageFile),
                    StandardCopyOption.REPLACE_EXISTING);

            // mark the successful loading of the ontology
            this.ontologyLoaded = true;

        } catch (BashException e) {
            String message = "The ontology contains invalid RDF (should be valid N3)";
            LOGGER.error(message, e);
            throw new DivideInvalidInputException(message, e);

        } catch (IOException e) {
            String message = "Unknown error during ontology loading";
            LOGGER.error(message, e);
            throw new DivideInitializationException(message, e);
        }
    }

    @Override
    public void registerQuery(IDivideQuery divideQuery,
                              IDivideQueryParser divideQueryParser)
            throws DivideQueryDeriverException, DivideInvalidInputException {
        // do nothing if given DIVIDE query is null
        if (divideQuery == null) {
            return;
        }

        // only proceed if query with this name does not exist yet
        // (will not happen since this is already checked before)
        if (!divideQueryMap.containsKey(divideQuery.getName())) {
            // validate different fields of query with EYE to ensure they contain valid N3
            validateEyeInput(false, divideQuery.getQueryPattern(),
                    divideQuery.getSensorQueryRule(), divideQuery.getGoal());

            // copy EYE DIVIDE query parts to files according to standard file
            // templates, substituted with the query name as parent directory
            String queryPatternPath = writeToDivideDirectory(
                    divideQuery.getQueryPattern(), String.format(
                            EYE_DIVIDE_QUERY_QUERY_PATTERN_PATH_TEMPLATE,
                            divideQuery.getName()));
            String sensorQueryRulePath = writeToDivideDirectory(
                    divideQuery.getSensorQueryRule(), String.format(
                            EYE_DIVIDE_QUERY_SENSOR_QUERY_RULE_PATH_TEMPLATE,
                            divideQuery.getName()));
            String goalPath = writeToDivideDirectory(
                    divideQuery.getGoal(), String.format(
                            EYE_DIVIDE_QUERY_GOAL_PATH_TEMPLATE,
                            divideQuery.getName()));

            // create corresponding EYE DIVIDE query keeping track of the
            // canonical paths of the different files
            EyeDivideQuery eyeDivideQuery = new EyeDivideQuery(
                    queryPatternPath, sensorQueryRulePath, goalPath);

            // save EYE DIVIDE query
            divideQueryMap.put(divideQuery.getName(), eyeDivideQuery);

            // process context enrichment:
            // if all context-enriching queries can be written as rules,
            // these rules will be appended to the sensor query rule file,
            // and the context enrichment can be removed from the DIVIDE query
            // -> so originally, the sensor query rule file content only consists
            //    of the sensor query rule itself
            if (divideQuery.getContextEnrichment() != null
                    && divideQuery.getContextEnrichment().getQueries() != null
                    && !divideQuery.getContextEnrichment().getQueries().isEmpty()) {
                // if the context enrichment contains at least one query,
                // check the possibility for each query to write it as a rule
                try {
                    LOGGER.info("REGISTER QUERY: trying to convert existing non-empty query context " +
                            "enrichment of query {} to a set of EYE rules", divideQuery.getName());

                    // first convert all queries to a string of rules
                    List<String> queryAsRuleList = new ArrayList<>();
                    for (ContextEnrichingQuery query : divideQuery.getContextEnrichment().getQueries()) {
                        String queryAsRule = convertQueryToEyeRule(query.getQuery(), divideQueryParser);

                        // validate rule as EYE input
                        validateEyeInput(false, queryAsRule);

                        // if valid, add to list of query rules
                        queryAsRuleList.add(queryAsRule);
                    }

                    // register context-enriching query rules to EYE DIVIDE query
                    // (by writing them to a temp file)
                    for (int i = 0; i < queryAsRuleList.size(); i++) {
                        String queryAsRule = queryAsRuleList.get(i);

                        // write context-enriching query rule to file
                        String contextEnrichingQueryRulePath = writeToDivideDirectory(
                                queryAsRule, String.format(
                                        EYE_DIVIDE_QUERY_CONTEXT_ENRICHING_QUERY_PATH_TEMPLATE,
                                        divideQuery.getName(), i + 1));

                        // register path to file to EYE DIVIDE query
                        eyeDivideQuery.addContextEnrichingQueryFilePath(contextEnrichingQueryRulePath);
                    }

                    // reset context enrichment of DIVIDE query to avoid that the DIVIDE engine
                    // executes these queries before starting the EYE query derivation
                    divideQuery.removeContextEnrichment();

                    LOGGER.info("REGISTER QUERY: existing non-empty query context enrichment of query {} " +
                            "is successfully converted to a set of EYE rules", divideQuery.getName());

                } catch (DivideInvalidInputException | DivideQueryDeriverException e) {
                    LOGGER.warn("REGISTER QUERY: existing non-empty query context enrichment of query {} " +
                            "cannot be converted to a set of EYE rules", divideQuery.getName());
                    // conversion not succeeded -> everything can be left as is:
                    // - the context enrichment of the query can stay there
                    // - the sensor query rule file content should not be extended with the rule string
                }
            }
        }
    }

    @Override
    public void unregisterQuery(IDivideQuery divideQuery) {
        // do nothing if given DIVIDE query is null
        if (divideQuery == null) {
            return;
        }

        // remove corresponding EYE DIVIDE query from map
        divideQueryMap.remove(divideQuery.getName());
    }

    @Override
    public IDivideQueryDeriverResult deriveQueries(String divideQueryName,
                                                   Context context,
                                                   String componentId)
            throws DivideNotInitializedException, DivideQueryDeriverException {
        if (!ontologyLoaded) {
            throw new DivideNotInitializedException(
                    "Ontology has not been loaded yet for the query deriver of this DIVIDE engine");
        }

        EyeDivideQuery eyeDivideQuery = divideQueryMap.get(divideQueryName);

        if (eyeDivideQuery != null) {
            try {
                LOGGER.debug(LogConstants.METRIC_MARKER, "DERIVE_QUERIES_START\t{}\t{}\t{}",
                        divideQueryName, componentId, context);
                LOGGER.debug(LogConstants.METRIC_MARKER, "DERIVE_QUERIES_START_OVERHEAD\t{}\t{}\t{}",
                        divideQueryName, componentId, context);

                // prepare context for query derivation
                Model preparedContext = prepareContextForQueryDerivation(context, divideQueryName);

                // put new context into temporary Turtle file (= N3 syntax) that
                // can be read by the query derivation
                String contextFile = writeToTempTurtleFile(preparedContext);

                // retrieve canonical paths of input files of EYE DIVIDE query
                String sensorQueryFile = eyeDivideQuery.getSensorQueryFilePath();
                String queryPatternFile = eyeDivideQuery.getQueryPatternFilePath();
                String queryGoalFile = eyeDivideQuery.getGoalFilePath();

                // create output directory for this query derivation
                String queryDerivationDirectoryPath = Paths.get(DIVIDE_DIRECTORY,
                        "query-derivation", componentId, divideQueryName,
                        formatter.format(new Date())).toString();

                // construct output files & create parent directories
                File proofFile = Paths.get(
                        queryDerivationDirectoryPath, "proof.n3").toFile();
                String proofFilePath = proofFile.getCanonicalPath();
                File extractedQueriesFile = Paths.get(
                        queryDerivationDirectoryPath, "extracted-queries.n3").toFile();
                String extractedQueriesFilePath = extractedQueriesFile.getCanonicalPath();
                File extractedWindowParametersFile = Paths.get(
                        queryDerivationDirectoryPath, "extracted-window-parameters.n3").toFile();
                String extractedWindowParametersFilePath = extractedWindowParametersFile.getCanonicalPath();
                File queriesAfterInputVariableSubstitutionFile = Paths.get(
                        queryDerivationDirectoryPath,
                        "queries-after-input-variable-substitution.n3").toFile();
                String queriesAfterInputVariableSubstitutionFilePath =
                        queriesAfterInputVariableSubstitutionFile.getCanonicalPath();
                File queriesAfterDynamicWindowParameterSubstitutionFile = Paths.get(
                        queryDerivationDirectoryPath,
                        "queries-after-dynamic-window-parameter-substitution.n3").toFile();
                String queriesAfterDynamicWindowParameterSubstitutionFilePath =
                        queriesAfterDynamicWindowParameterSubstitutionFile.getCanonicalPath();
                proofFile.getParentFile().mkdirs();

                // verify if new TBox definitions in context should be handled
                String usedImageFile;
                List<String> proofInputFiles = new ArrayList<>();
                if (handleTBoxDefinitionsInContext) {
                    // if handling new TBox definitions, a new image will be built
                    // from the prebuilt image, using the context data
                    // -> only the sensor query file is given as input to the proof
                    //    generation (since the context file is already contained in
                    //    the new image)
                    usedImageFile = generateNewImageFromContextWithPossibleTBoxDefinitions(contextFile);
                    proofInputFiles.add(sensorQueryFile);
                } else {
                    // if not handling new TBox definitions (= default), simply use
                    // the prebuilt image and use the sensor query file & context file
                    // as input for the proof generation
                    usedImageFile = imageFile;
                    proofInputFiles.add(sensorQueryFile);
                    proofInputFiles.add(contextFile);
                }
                // -> potential rule files representing context-enriching queries
                //    are also added to input files for proof generation
                proofInputFiles.addAll(eyeDivideQuery.getContextEnrichingQueryFilePaths());
                LOGGER.debug(LogConstants.METRIC_MARKER, "DERIVE_QUERIES_END_OVERHEAD\t{}\t{}\t{}",
                        divideQueryName, componentId, context);

                // construct proof towards goal
                LOGGER.debug(LogConstants.METRIC_MARKER, "DERIVE_QUERIES_START_REASONING\t{}\t{}\t{}",
                        divideQueryName, componentId, context);
                EyeReasoner.runFromImageToFile(
                        usedImageFile, proofInputFiles, queryGoalFile, proofFilePath, null);
                LOGGER.debug(LogConstants.METRIC_MARKER, "DERIVE_QUERIES_END_REASONING\t{}\t{}\t{}",
                        divideQueryName, componentId, context);

                // extract queries from proof
                LOGGER.debug(LogConstants.METRIC_MARKER, "DERIVE_QUERIES_START_EXTRACTION\t{}\t{}\t{}",
                        divideQueryName, componentId, context);
                List<String> queryExtractionInputFiles = Arrays.asList(
                        proofFilePath, contextFile);
                EyeReasoner.runToFile(
                        queryExtractionInputFiles, queryExtractionGoalFile,
                        extractedQueriesFilePath, queryExtractionOptions);

                // extract window parameters from proof
                List<String> windowParameterExtractionInputFiles = Arrays.asList(
                        proofFilePath, contextFile);
                EyeReasoner.runToFile(
                        windowParameterExtractionInputFiles, windowParameterExtractionGoalFile,
                        extractedWindowParametersFilePath, windowParameterExtractionOptions);
                LOGGER.debug(LogConstants.METRIC_MARKER, "DERIVE_QUERIES_END_EXTRACTION\t{}\t{}\t{}",
                        divideQueryName, componentId, context);

                // substitute input variables of extracted queries in query patterns
                LOGGER.debug(LogConstants.METRIC_MARKER, "DERIVE_QUERIES_START_INPUT_SUBSTITUTION\t{}\t{}\t{}",
                        divideQueryName, componentId, context);
                List<String> inputVariableSubstitutionInputFiles = Arrays.asList(
                        queryPatternFile,
                        extractedQueriesFilePath,
                        extractedWindowParametersFilePath,
                        queryInputVariableSubstitutionRulesFile,
                        queryInputVariableSubstitutionSupportedDatatypesFile);
                EyeReasoner.runToFile(
                        inputVariableSubstitutionInputFiles,
                        queryInputVariableSubstitutionGoalFile,
                        queriesAfterInputVariableSubstitutionFilePath,
                        querySubstitutionOptions);
                LOGGER.debug(LogConstants.METRIC_MARKER, "DERIVE_QUERIES_END_INPUT_SUBSTITUTION\t{}\t{}\t{}",
                        divideQueryName, componentId, context);

                // create an intermediate query derivation result
                // -> send this intermediate query derivation result to the window parameter substitution
                LOGGER.debug(LogConstants.METRIC_MARKER, "DERIVE_QUERIES_START_WINDOW_SUBSTITUTION\t{}\t{}\t{}",
                        divideQueryName, componentId, context);
                EyeDivideQueryDeriverIntermediateResult eyeDivideQueryDeriverIntermediateResult =
                        new EyeDivideQueryDeriverIntermediateResult(
                                queriesAfterInputVariableSubstitutionFilePath,
                                queriesAfterDynamicWindowParameterSubstitutionFilePath);
                EyeDivideQueryDeriverResult result = substituteWindowParametersInQuery(
                        eyeDivideQueryDeriverIntermediateResult,
                        SubstitutionTrigger.CONTEXT_CHANGE);
                LOGGER.debug(LogConstants.METRIC_MARKER, "DERIVE_QUERIES_END_WINDOW_SUBSTITUTION\t{}\t{}\t{}",
                        divideQueryName, componentId, context);
                LOGGER.debug(LogConstants.METRIC_MARKER, "DERIVE_QUERIES_END\t{}\t{}\t{}",
                        divideQueryName, componentId, context);
                return result;

            } catch (BashException | IOException e) {
                throw new DivideQueryDeriverException(e);
            }

        } else {
            LOGGER.warn("Calling the DIVIDE query derivation for an unknown (unregistered) " +
                    "DIVIDE query with name '{}'", divideQueryName);
            // return empty list of derived queries, since the derivation was called for
            // an unknown DIVIDE query
            return new EyeDivideQueryDeriverResult();
        }
    }

    @Override
    public IDivideQueryDeriverResult substituteWindowParameters(String divideQueryName,
                                                                Model windowParameters,
                                                                String componentId,
                                                                IDivideQueryDeriverResult lastResult)
            throws DivideQueryDeriverException, DivideNotInitializedException {
        if (!ontologyLoaded) {
            throw new DivideNotInitializedException(
                    "Ontology has not been loaded yet for the query deriver of this DIVIDE engine");
        }

        if (lastResult == null) {
            throw new DivideQueryDeriverException(
                    "No valid result was passed to do the window parameter substitution");
        }

        try {
            // write new window parameters to temp file
            String windowParametersFile = writeToTempTurtleFile(windowParameters);

            // cast the last result to a result of this EYE query deriver
            EyeDivideQueryDeriverResult eyeDivideQueryDeriverResult =
                    (EyeDivideQueryDeriverResult) lastResult;

            // ensure the last result is valid
            if (eyeDivideQueryDeriverResult.getIntermediateResult() == null) {
                throw new DivideQueryDeriverException(
                        "No valid result was passed to do the window parameter substitution");
            }

            // do the window parameter substitution again,
            // starting from the intermediate query deriver result
            return substituteWindowParametersInQuery(
                    eyeDivideQueryDeriverResult.getIntermediateResult(),
                    SubstitutionTrigger.MONITOR,
                    windowParametersFile);

        } catch (BashException | IOException e) {
            throw new DivideQueryDeriverException(e);
        }
    }

    public EyeDivideQueryDeriverResult substituteWindowParametersInQuery(
            EyeDivideQueryDeriverIntermediateResult eyeDivideQueryDeriverIntermediateResult,
            SubstitutionTrigger trigger,
            String... extraInputFiles) throws IOException, BashException {
        // substitute dynamic window parameters of extracted queries in query patterns
        List<String> dynamicWindowParameterSubstitutionInputFiles = new ArrayList<>();
        dynamicWindowParameterSubstitutionInputFiles.addAll(Arrays.asList(
                eyeDivideQueryDeriverIntermediateResult.
                        getQueriesAfterInputVariableSubstitutionFilePath(),
                queryDynamicWindowParameterSubstitutionRulesFile,
                substitutionTriggerFilePathMap.get(trigger)));
        dynamicWindowParameterSubstitutionInputFiles.addAll(Arrays.asList(extraInputFiles));
        EyeReasoner.runToFile(
                dynamicWindowParameterSubstitutionInputFiles,
                queryDynamicWindowParameterSubstitutionGoalFile,
                eyeDivideQueryDeriverIntermediateResult.
                        getQueriesAfterDynamicWindowParameterSubstitutionFilePath(),
                querySubstitutionOptions);

        // substitute static window parameters of extracted queries in query patterns
        List<String> staticWindowParameterSubstitutionInputFiles = Arrays.asList(
                eyeDivideQueryDeriverIntermediateResult.
                        getQueriesAfterDynamicWindowParameterSubstitutionFilePath(),
                queryStaticWindowParameterSubstitutionRulesFile);
        String queriesAfterStaticWindowParameterSubstitution = EyeReasoner.run(
                staticWindowParameterSubstitutionInputFiles,
                queryStaticWindowParameterSubstitutionGoalFile,
                querySubstitutionOptions);

        // convert substituted queries (in N3 = Turtle format) to Jena model
        Model substitutedQueriesModel = JenaUtilities.parseString(
                queriesAfterStaticWindowParameterSubstitution, RDFLanguage.TURTLE);

        // convert queries to individual RSP-QL query strings
        EyeDivideQueryConverter queryConverter = new EyeDivideQueryConverter(this);
        List<String> convertedQueries = queryConverter.getQueries(substitutedQueriesModel);

        // create and return a query derivation result
        return new EyeDivideQueryDeriverResult(
                eyeDivideQueryDeriverIntermediateResult,
                queriesAfterStaticWindowParameterSubstitution,
                substitutedQueriesModel,
                convertedQueries);
    }

    private Model prepareContextForQueryDerivation(Context context,
                                                   String divideQueryName) {
        long start = System.currentTimeMillis();

        Model model = context.getContext();
        UpdateAction.execute(sparqlQueryPrepareContextInitial, model);
        Model copy = ModelFactory.createDefaultModel();
        while (model.size() != copy.size()) {
            copy = ModelFactory.createDefaultModel();
            copy.add(model);
            UpdateAction.execute(sparqlQueryPrepareContextLoop, model);
        }

        long end = System.currentTimeMillis();
        LOGGER.info("Prepared context for DIVIDE query {} and context {} in {} seconds",
                divideQueryName, context.getId(), (end - start));

        return model;
    }

    private String generateNewImageFromContextWithPossibleTBoxDefinitions(String contextFile)
            throws IOException, BashException {
        // generate temporary files for outputs of reasoner
        String triplesFile = File.createTempFile("triples", ".ttl").getCanonicalPath();
        String rulesFile = File.createTempFile("rules", ".ttl").getCanonicalPath();
        String newImageFile = File.createTempFile("ype", ".pvm").getCanonicalPath();

        // generate new triples from applying all OWL-RL rules to image (with original
        // TBox) and new context
        EyeReasoner.runFromImageToFile(
                imageFile,
                Arrays.asList(contextFile, preprocessingListsFile,
                        preprocessingInstantiateTriplesFile),
                triplesFile,
                preprocessingTripleCreationOptions);

        // generate new rules from the set of new triples
        EyeReasoner.runToFile(
                Collections.singletonList(triplesFile),
                preprocessingInstantiateRulesFile,
                rulesFile,
                preprocessingRuleCreationOptions);

        // create new image based on new triples and rules
        EyeReasoner.runToImage(
                Arrays.asList(triplesFile, rulesFile),
                newImageFile);

        return newImageFile;
    }

    synchronized void saveConvertedPrefixesString(String uri, String converted) {
        this.convertedPrefixesMap.put(uri, converted);
    }

    /**
     * @return converted prefix string for the given URI if this has already
     *         been registered with the {@link #saveConvertedPrefixesString(String, String)}
     *         method; null otherwise
     */
    synchronized String retrieveConvertedPrefixesString(String uri) {
        return this.convertedPrefixesMap.get(uri);
    }

    /**
     * Checks whether the EYE input is invalid or not.
     * To be valid, an input should be valid N3 and should not be empty (unless it is
     * specified that an empty can be empty)
     *
     * @param allowEmpty specifies whether the inputs can be empty or not
     *                   (if not, a DivideInvalidInputException will be thrown if any
     *                    of the inputs is empty)
     * @param inputs string inputs to be validated (NOT files, but the actual inputs themselves)
     * @throws DivideInvalidInputException if any of the inputs contains invalid N3, or if any
     *                                     of the inputs is empty while the allowEmpty parameter
     *                                     is set to false
     * @throws DivideQueryDeriverException if something went wrong during the validation, making it
     *                                     impossible to validate this
     */
    @SuppressWarnings("SameParameterValue")
    private void validateEyeInput(boolean allowEmpty, String... inputs)
            throws DivideInvalidInputException, DivideQueryDeriverException {
        try {
            // write all inputs to temporary files
            List<String> inputFiles = new ArrayList<>();
            for (String input : inputs) {
                // check if empty when not allowed
                if (!allowEmpty && input.trim().isEmpty()) {
                    throw new DivideInvalidInputException("Some of the inputs are empty");
                }

                String tempFile = IOUtilities.writeToTempFile(
                        input,
                        "input-" + UUID.randomUUID() + "-" + System.currentTimeMillis(), ".ttl");
                if (tempFile == null) {
                    throw new IOException("Error when writing input to temp file for validation");
                }
                inputFiles.add(tempFile);
            }

            // simply read in input files with EYE reasoner, and output similarly
            // to how the ontology is outputted
            // => EYE input reading step will already fail if the input is invalid
            //    and therefore generate a BashException
            EyeReasoner.run(
                    inputFiles,
                    preprocessingOntologyCreationOptions);

        } catch (BashException e) {
            String message = "Some of the inputs contain invalid RDF (should be valid N3)";
            LOGGER.error(message, e);
            throw new DivideInvalidInputException(message, e);

        } catch (IOException e) {
            String message = "Unknown error during validation of query input";
            LOGGER.error(message, e);
            throw new DivideQueryDeriverException(message, e);
        }
    }

    private String copyResourceToDivideDirectory(String resource) throws DivideQueryDeriverException {
        // read resource into content string
        String content = readResource(resource);

        // write content string to DIVIDE directory on same path as resource
        // and return canonical path of this new file
        return writeToDivideDirectory(content, resource);
    }

    private String readResource(String resource) {
        return IOUtilities.readFileIntoString(
                getClass().getResourceAsStream(String.format("/%s", resource)));
    }

    /**
     * @return canonical path of file written to DIVIDE directory
     */
    private String writeToDivideDirectory(String content, String relativePath)
            throws DivideQueryDeriverException {
        try {
            // create file objects and create required parent directories
            // (if not existing yet)
            File file = new File(DIVIDE_DIRECTORY, relativePath);
            file.getParentFile().mkdirs();

            // get canonical path of new file
            String canonicalPath = file.getCanonicalPath();

            // write content to new file
            IOUtilities.writeToFile(content, canonicalPath);

            return canonicalPath;

        } catch (IOException e) {
            throw new DivideQueryDeriverException(e);
        }
    }

    /**
     * @return canonical path of created temporary Turtle file
     */
    private String writeToTempTurtleFile(Model triples) throws IOException {
        String tempFile = IOUtilities.writeToTempFile(
                JenaUtilities.serializeModel(triples, RDFLanguage.TURTLE),
                "triples-" + UUID.randomUUID() + "-" + System.currentTimeMillis(), ".ttl");

        if (tempFile == null) {
            throw new IOException("Error when writing data to temp file");
        }

        return tempFile;
    }



    // EYE QUERY TO RULE CONVERSION

    private static final String SENSOR_QUERY_RULE_ADDITIONAL_RULE_TEMPLATE =
            "%s\n\n{\n%s\n}\n=>\n{\n%s\n} .";

    private String convertQueryToEyeRule(String query, IDivideQueryParser divideQueryParser)
            throws DivideQueryDeriverException {
        try {
            // parse query
            ParsedSparqlQuery parsedSparqlQuery = divideQueryParser.parseSparqlQuery(query);
            SplitSparqlQuery splitSparqlQuery = parsedSparqlQuery.getSplitSparqlQuery();

            // check if query defines a dynamic window parameter
            // -> if so, it cannot be rewritten as a rule since then the idea of "adding an empty
            //    list of dynamic window parameters when having none defined in the context after
            //    query derivation" can no longer be applied
            String sdPrefix = "%%%INVALID SPARQL SEQUENCE%%%";
            String sdQueryPrefix = "%%%INVALID SPARQL SEQUENCE%%%";
            for (Prefix prefix : parsedSparqlQuery.getPrefixes()) {
                if ("<http://idlab.ugent.be/sensdesc#>".equals(prefix.getUri())) {
                    sdPrefix = prefix.getName();
                }
                if ("<http://idlab.ugent.be/sensdesc/query#>".equals(prefix.getUri())) {
                    sdQueryPrefix = prefix.getName();
                }
            }
            if (Pattern.compile(String.format(
                    "((%spattern)|(<http://idlab.ugent.be/sensdesc/query#pattern>))\\s+" +
                            "((%swindowParameters)|(<http://idlab.ugent.be/sensdesc#windowParameters>))",
                            sdQueryPrefix, sdPrefix)).
                    matcher(splitSparqlQuery.getResultPart()).find()) {
                String message = "Context-enriching queries contain at " +
                        "least one query defining dynamic window parameters";
                LOGGER.warn(message);
                throw new DivideQueryDeriverException(message);
            }

            // convert query to rule
            return String.format(SENSOR_QUERY_RULE_ADDITIONAL_RULE_TEMPLATE,
                    divideQueryParser.getTurtlePrefixList(parsedSparqlQuery.getPrefixes()),
                    splitSparqlQuery.getWherePart(),
                    splitSparqlQuery.getResultPart());

        } catch (InvalidDivideQueryParserInputException e) {
            throw new DivideQueryDeriverException(e);
        }
    }

}
