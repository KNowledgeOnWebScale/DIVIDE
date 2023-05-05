package be.ugent.idlab.divide.core.query.parser;

import be.ugent.idlab.divide.core.context.ContextEnrichingQuery;
import be.ugent.idlab.divide.core.context.ContextEnrichment;
import be.ugent.idlab.util.io.IOUtilities;
import be.ugent.idlab.util.rdf.RDFLanguage;
import be.ugent.idlab.util.rdf.jena3.owlapi4.JenaUtilities;
import org.apache.jena.atlas.lib.Pair;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QueryParseException;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.sparql.syntax.ElementPathBlock;
import org.apache.jena.sparql.syntax.ElementVisitorBase;
import org.apache.jena.sparql.syntax.ElementWalker;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class DivideQueryParser implements IDivideQueryParser {

    private static final boolean DEBUG = false;
    
    private static int PREFIX_COUNTER = 0;

    private static final Pattern PREFIX_PATTERN = Pattern.compile(
            "(\\s*PREFIX\\s+(\\S+)\\s+(<[^<>]+>))", Pattern.CASE_INSENSITIVE);

    private static final Pattern SPARQL_FROM_NAMED_GRAPH_PATTERN = Pattern.compile(
            "\\s*FROM\\s+NAMED\\s+(\\S+)", Pattern.CASE_INSENSITIVE);

    private static final Pattern SPARQL_FROM_DEFAULT_GRAPH_PATTERN = Pattern.compile(
            "\\s*FROM\\s+(\\S+)", Pattern.CASE_INSENSITIVE);

    private static final Pattern RSP_QL_FROM_NAMED_GRAPH_PATTERN = Pattern.compile(
            "\\s*FROM\\s+NAMED\\s+GRAPH\\s+(\\S+)", Pattern.CASE_INSENSITIVE);

    private static final Pattern RSP_QL_FROM_DEFAULT_GRAPH_PATTERN = Pattern.compile(
            "\\s*FROM\\s+GRAPH\\s+(\\S+)", Pattern.CASE_INSENSITIVE);

    private static final Pattern RSP_QL_FROM_NAMED_WINDOW_PATTERN = Pattern.compile(
            "\\s*FROM\\s+NAMED\\s+WINDOW\\s+(\\S+)\\s+ON\\s+(\\S+)\\s+\\[([^\\[\\]]+)]",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern RSP_QL_WINDOW_PARAMETERS_PATTERN = Pattern.compile(
            "\\s*((RANGE\\s+(\\S+))|(FROM\\s+NOW-(\\S+)\\s+TO\\s+NOW-(\\S+)))\\s+(TUMBLING|(STEP\\s+(\\S+)))",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern SPARQL_WHERE_CLAUSE_GRAPH_PATTERN = Pattern.compile(
            "\\s*(GRAPH)\\s+(\\S+)\\s+\\{", Pattern.CASE_INSENSITIVE);

    private static final Pattern RSP_QL_WHERE_CLAUSE_GRAPH_OR_WINDOW_PATTERN = Pattern.compile(
            "\\s*(WINDOW|GRAPH)\\s+(\\S+)\\s+\\{", Pattern.CASE_INSENSITIVE);

    private static final Pattern SPARQL_QUERY_SPLIT_PATTERN = Pattern.compile(
            "^\\s*" + // beginning of query string
                    "(" + PREFIX_PATTERN.pattern() + "*)" + // prefix group 1
                    "\\s+(CONSTRUCT|SELECT|ASK|DESCRIBE)((.(?!FROM))*)" + // form group 5, result group 6
                    "(\\s*(FROM(.(?!WHERE))+)*)\\s*" + // from clauses group 8
                    "(WHERE\\s*\\{(.+)})" + // where clause group 12
                    "([^{}]*)" + // remainder group 13
                    "\\s*$", // end of query string
            Pattern.CASE_INSENSITIVE);

    private static final Pattern SPECIAL_SPARQL_PATTERN =
            Pattern.compile("(OPTIONAL|UNION|GRAPH|BIND|GROUP BY|HAVING|MINUS|FILTER)" +
                            "(.(?!(OPTIONAL|UNION|GRAPH|BIND|GROUP BY|HAVING|MINUS|FILTER)))+",
                    Pattern.CASE_INSENSITIVE);

    private static final Pattern GROUP_BY_PATTERN =
            Pattern.compile("GROUP\\s+BY\\s+(.(?!ORDER|LIMIT|OFFSET))+", Pattern.CASE_INSENSITIVE);

    private static final Pattern PN_CHARS_BASE_PATTERN =
            Pattern.compile("([A-Z]|[a-z]|[\u00C0-\u00D6]|[\u00D8-\u00F6]|[\u00F8-\u02FF]|" +
                    "[\u0370-\u037D]|[\u037F-\u1FFF]|[\u200C-\u200D]|[\u2070-\u218F]|[\u2C00-\u2FEF]|" +
                    "[\u3001-\uD7FF]|[\uF900-\uFDCF]|[\uFDF0-\uFFFD]|[\\x{10000}-\\x{EFFFF}])");
    private static final Pattern PN_CHARS_U_PATTERN =
            Pattern.compile(String.format("(%s)|_", PN_CHARS_BASE_PATTERN));
    private static final Pattern PN_CHARS_PATTERN =
            Pattern.compile(String.format(
                    "(%s)|-|[0-9]|\u00B7|[\u0300-\u036F]|[\u203F-\u2040]", PN_CHARS_U_PATTERN));
    private static final Pattern PN_PREFIX_PATTERN =
            Pattern.compile(String.format("(%s)(((%s)|'.')*(%s))?",
                    PN_CHARS_BASE_PATTERN, PN_CHARS_PATTERN, PN_CHARS_PATTERN));
    private static final Pattern PN_NAME_NS_PATTERN =
            Pattern.compile(String.format("(\\s|\\(|^|\\^)((%s)?:)", PN_PREFIX_PATTERN));
    private static final Pattern VARNAME_PATTERN =
            Pattern.compile(String.format(
                    "((%s)|[0-9])((%s)|[0-9]|\u00B7|[\u0300-\u036F]|[\u203F-\u2040])*",
                    PN_CHARS_U_PATTERN, PN_CHARS_U_PATTERN));
    private static final Pattern VAR1_PATTERN =
            Pattern.compile(String.format("\\?(%s)", VARNAME_PATTERN));

    private static final Pattern USED_PREFIX_PATTERN = PN_NAME_NS_PATTERN;
    private static final Pattern UNBOUND_VARIABLES_PATTERN = VAR1_PATTERN;
    private static final Pattern UNBOUND_VARIABLES_IN_STREAM_WINDOW_PATTERN =
            Pattern.compile(String.format("\\?\\{(%s)}", VARNAME_PATTERN));

    private static final Pattern STREAM_WINDOW_PARAMETER_VARIABLE_PATTERN =
            Pattern.compile(String.format("((%s)|(PT(%s)([SMH])))",
                    UNBOUND_VARIABLES_IN_STREAM_WINDOW_PATTERN,
                    UNBOUND_VARIABLES_IN_STREAM_WINDOW_PATTERN));
    private static final Pattern STREAM_WINDOW_PARAMETER_NUMBER_PATTERN =
            Pattern.compile("(PT(\\d+)([SMH]))");

    private static final Pattern SELECT_CLAUSE_EXPRESSION_PATTERN =
            Pattern.compile(String.format("\\(\\s*(\\S+)\\s+AS\\s+(%s)\\s*\\)", VAR1_PATTERN));
    private static final Pattern SELECT_CLAUSE_PATTERN_ENTRY =
            Pattern.compile(String.format("((%s)|(%s))\\s+",
                    SELECT_CLAUSE_EXPRESSION_PATTERN, VAR1_PATTERN));
    private static final Pattern SELECT_CLAUSE_PATTERN =
            Pattern.compile(String.format("(%s)+", SELECT_CLAUSE_PATTERN_ENTRY));

    private static final List<String> POSSIBLE_WHERE_CLAUSE_SPARQL_KEYWORDS = new ArrayList<>();

    static {
        POSSIBLE_WHERE_CLAUSE_SPARQL_KEYWORDS.add("optional");
        POSSIBLE_WHERE_CLAUSE_SPARQL_KEYWORDS.add("union");
        POSSIBLE_WHERE_CLAUSE_SPARQL_KEYWORDS.add("graph");
        POSSIBLE_WHERE_CLAUSE_SPARQL_KEYWORDS.add("bind");
        POSSIBLE_WHERE_CLAUSE_SPARQL_KEYWORDS.add("group by");
        POSSIBLE_WHERE_CLAUSE_SPARQL_KEYWORDS.add("having");
        POSSIBLE_WHERE_CLAUSE_SPARQL_KEYWORDS.add("minus");
        POSSIBLE_WHERE_CLAUSE_SPARQL_KEYWORDS.add("filter");
    }

    private final DivideQueryGenerator divideQueryGenerator;
    private final boolean processUnmappedVariableMatches;
    private final boolean validateUnboundVariablesInRspQlQueryBody;

    DivideQueryParser(boolean processUnmappedVariableMatches,
                      boolean validateUnboundVariablesInRspQlQueryBody) {
        this.divideQueryGenerator = new DivideQueryGenerator();

        this.processUnmappedVariableMatches = processUnmappedVariableMatches;
        this.validateUnboundVariablesInRspQlQueryBody = validateUnboundVariablesInRspQlQueryBody;

        // initialize Jena
        org.apache.jena.query.ARQ.init();
    }

    DivideQueryParser() {
        this(true, true);
    }

    @Override
    public void validateDivideQueryContextEnrichment(ContextEnrichment contextEnrichment)
            throws InvalidDivideQueryParserInputException {
        // check all context-enriching queries
        for (ContextEnrichingQuery query : contextEnrichment.getQueries()) {
            // split query
            SplitSparqlQuery splitSparqlQuery = splitSparqlQuery(" " + query.getQuery());

            // ensure query is of CONSTRUCT form
            if (splitSparqlQuery.getQueryForm() != QueryForm.CONSTRUCT) {
                throw new InvalidDivideQueryParserInputException(
                        "Context-enriching query should be of CONSTRUCT form");
            }

            // ensure query does not contain any FROM clauses
            if (splitSparqlQuery.getFromPart() != null &&
                    !splitSparqlQuery.getFromPart().trim().isEmpty()) {
                throw new InvalidDivideQueryParserInputException(
                        "Context-enriching query should not contain any FROM clauses");
            }

            // ensure query does not contain any final part (solution modifiers)
            if (splitSparqlQuery.getFinalPart() != null &&
                    !splitSparqlQuery.getFinalPart().trim().isEmpty()) {
                throw new InvalidDivideQueryParserInputException(
                        "Context-enriching query should not contain any solution modifiers");
            }

            // ensure query is valid SPARQL
            try (QueryExecution queryExecution = QueryExecutionFactory.create(
                    query.getQuery(), ModelFactory.createDefaultModel())) {
                queryExecution.execConstruct();
            } catch (Exception e) {
                throw new InvalidDivideQueryParserInputException(
                        "Context-enriching query should be valid SPARQL");
            }
        }
    }

    @Override
    public DivideQueryParserOutput parseDivideQuery(DivideQueryParserInput input)
            throws InvalidDivideQueryParserInputException {
        // make sure the input is validated & preprocessed
        // (because the remainder of the parsing assumes valid & preprocessed input)
        input.validate();
        input.preprocess();

        // process variable mapping between stream & final query if relevant
        MappedDivideQueryParserInput mappedInput =
                processStreamToFinalQueryVariableMappings(input);

        // clean input: replace overlapping variables with new non-overlapping ones
        CleanDivideQueryParserInput cleanInput =
                cleanInputFromOverlappingVariables(mappedInput);

        DivideQueryParserOutput result;
        if (input.getInputQueryLanguage() == InputQueryLanguage.SPARQL) {
            result = parseDivideQueryFromSparqlQueries(cleanInput);
        } else if (input.getInputQueryLanguage() == InputQueryLanguage.RSP_QL) {
            result = parseDivideQueryFromRspQlQuery(cleanInput);
        } else {
            // should not be possible
            throw new InvalidDivideQueryParserInputException(
                    "Invalid input query language");
        }

        // process output again, based on variable mapping
        result = restoreOriginalVariablesInOutput(result, cleanInput.getVariableMapping());

        // increase the counter of the generator which is used to create unique
        // pattern and prefixes IRIs
        DivideQueryGenerator.COUNTER++;
        return result;
    }

    private MappedDivideQueryParserInput processStreamToFinalQueryVariableMappings(
            DivideQueryParserInput input) throws InvalidDivideQueryParserInputException {
        // check if mappings should be analyzed: is the case for SPARQL query input where
        // a final query is present
        // NOTE: analyzing is also required with an empty mapping, to check all variable
        //       matches that are not defined in the mapping
        boolean mappingAnalysisRequired =
                input.getInputQueryLanguage() == InputQueryLanguage.SPARQL &&
                input.getFinalQuery() != null && !input.getFinalQuery().trim().isEmpty();

        // if no mapping analysis is required, we can continue with the original input
        if (!mappingAnalysisRequired) {
            return new MappedDivideQueryParserInput(input);
        }

        print("PROCESSING STREAM TO FINAL QUERY VARIABLE MAPPINGS");

        // validate final query
        String finalQuery = input.getFinalQuery();
        validateSparqlQuery(finalQuery, "Final");

        // split final query to be used further on
        SplitSparqlQuery splitFinalQuery = splitSparqlQuery(finalQuery);

        // retrieve mapping
        Map<String, String> mapping = input.getStreamToFinalQueryVariableMapping();

        // further check mapping in case of ASK query
        // -> for ASK queries, the result part is empty, so there is no part of
        //    the final query that will end up in the RSP-QL query body
        // -> no mapping should be done
        if (splitFinalQuery.getQueryForm() == QueryForm.ASK) {
            // so in case the mapping is empty, we can continue with the original input
            // -> if not, this is an indication of wrong input
            if (mapping.isEmpty()) {
                return new MappedDivideQueryParserInput(input);
            } else {
                throw new InvalidDivideQueryParserInputException(
                        "No stream to final query variable mapping should be provided " +
                                "if the final query is an ASK query.");
            }
        }

        // IF THIS POINT IS REACHED, A VARIABLE MATCH & MAPPING CHECK SHOULD BE DONE
        // -> based on the mappings, the stream and final query should both be analyzed
        // -> if adaptations to variable names are required, only the final query will
        //    be updated
        // BUT: what about variables occurring in other input parts?
        // -> solution modifier: this is used in the final RSP-QL query, of which the
        //                       WHERE clause is fully extracted from the stream query
        // -> stream windows: variables occurring in the stream windows should always be
        //                    replaced as window parameter during the query derivation, so
        //                    they should either occur in the stream part of the stream
        //                    query, or they are just put there to allow replacement of the
        //                    default window parameter value via context-enriching queries
        // -> intermediate queries: they are used separately as extra rules in addition
        //                          to the sensor query rule, but not used in the sensor
        //                          query rule so no matching is required of them
        // CONCLUSION: if no updates are made to the variables as how they occur in the
        //             stream query, then no updates are required to the variables occurring
        //             in the solution modifier, stream windows & intermediate queries
        // => to align all matches and remove identical variable names for non-matches,
        //    it suffices to only make updates to variable names in final query

        // extract all variables occurring in stream query and final query
        List<String> streamQueryVariables = findUnboundVariables(input.getStreamQuery());
        List<String> finalQueryVariables = findUnboundVariables(input.getFinalQuery());

        // check if all variable mappings are valid, i.e. whether all keys are variable
        // names in the stream query, and all values are variable names in final query
        if (!new HashSet<>(streamQueryVariables).containsAll(mapping.keySet())) {
            throw new InvalidDivideQueryParserInputException(
                    "Stream to final query variable mapping contains variable " +
                            "names that do not occur in stream query");
        }
        if (!new HashSet<>(finalQueryVariables).containsAll(mapping.values())) {
            throw new InvalidDivideQueryParserInputException(
                    "Stream to final query variable mapping contains variable " +
                            "names that do not occur in final query");
        }

        // check if mapping file contains no conflicts
        Set<String> mappingValues = new HashSet<>();
        for (String s : mapping.keySet()) {
            if (mappingValues.contains(mapping.get(s))) {
                throw new InvalidDivideQueryParserInputException(
                        String.format("Stream to final query variable mapping contains " +
                                "duplicate mapping to variable '%s'", mapping.get(s)));
            }
            mappingValues.add(mapping.get(s));
        }

        // create reverse mapping to know the required replacements from the point of
        // view of the final query
        Map<String, String> reverseMapping = new HashMap<>();
        for (String s : mapping.keySet()) {
            reverseMapping.put(mapping.get(s), s);
        }

        // keep track of list of required variable replacements in final query
        Map<String, String> requiredReplacements = new HashMap<>();

        // create set of all possible conflicting variables to ensure that
        // no conflicts are created with the newest variables
        // -> this set should of course contain all final query variables
        //    (both old and new)
        // -> but also all stream query variables, to avoid a potential replacement
        //    of a variable in the final query to an already existing variable in the
        //    stream query
        // (note that conflicts are very unlikely because new random variable names are
        //  obtained from a random UUID generator, but it is still better to be safe)
        Set<String> conflictingVariables = new HashSet<>(finalQueryVariables);
        conflictingVariables.addAll(streamQueryVariables);

        // loop over all variables occurring in the final query
        // -> a replacement entry should be created for ALL variables
        //    (also the ones that should not be actually replaced: for them,
        //     a replacement to themselves should be created)
        for (String finalQueryVariable : finalQueryVariables) {
            if (reverseMapping.containsKey(finalQueryVariable)) {
                // if the variable has a defined mapping, the required replacement in
                // the final query is obvious
                requiredReplacements.put(
                        finalQueryVariable, reverseMapping.get(finalQueryVariable));
                conflictingVariables.add(reverseMapping.get(finalQueryVariable));
                print("Add defined mapping: " + finalQueryVariable +
                        " to " + reverseMapping.get(finalQueryVariable));

            } else if (streamQueryVariables.contains(finalQueryVariable) &&
                    (mapping.containsKey(finalQueryVariable) || !processUnmappedVariableMatches)) {
                // if the final query variable also occurs in the stream query, and there
                // is no specifically defined variable in the stream query to which this
                // matches, then it depends on 2 things to decide whether this variable
                // should be replaced:
                // 1. if the variable also occurs as key of the mapping, then it should be
                //    replace by a random new variable, because there will be another final
                //    variable that is replaced by this variable
                // 2. if not, then the variable does not occur in the mapping (not in the key set
                //    if condition 1 above is not fulfilled, and not in the value set since
                //    the reverse mapping's key set does not contain this variable)
                //    -> then it depends on how to handle unmapped matches: if unmapped variable
                //       matches should not be processed, this means that they cannot be considered
                //       as a match, even though their names happen to be identical
                //    -> then a replacement is also required
                // (otherwise, they can be considered as a match, and this means they
                //  can be left unchanged)

                // -> the final query variable should be replaced to a new variable
                //    that is not occurring in the stream query, and that is also not
                //    yet occurring in the final query
                boolean variableAccepted = false;
                while (!variableAccepted) {
                    String triedNewVariable = generateRandomUnboundVariable();
                    // there may be no final query variable that equals this name or
                    // of which the new variable is a substring
                    variableAccepted = conflictingVariables
                            .stream()
                            .noneMatch(s -> s.equals(triedNewVariable) ||
                                    s.contains(triedNewVariable));
                    if (variableAccepted) {
                        requiredReplacements.put(
                                finalQueryVariable, triedNewVariable);
                        conflictingVariables.add(triedNewVariable);
                        print("Add additional mapping: " +
                                finalQueryVariable + " to " + triedNewVariable);
                    }
                }

            } else {
                // if it's a variable that is not occurring in the stream query, and also not
                // a variable that should be mapped, then it can be left as is
                // -> a replacement to itself should then be created
                requiredReplacements.put(finalQueryVariable, finalQueryVariable);
            }
        }

        // split replacement list in two to first do some temporal replacements
        // -> these replacements will be done first before doing the actual replacements
        // -> this is to avoid that conflicts occur with cross-referenced mappings, e.g.,
        //    where ?a is mapped to ?b and ?b is mapped to ?a
        // -> this works if the resulting variables after replacement are unique, i.e.,
        //    they do not occur as such in the list of variables or as a substring of any
        //    of these variables
        Map<String, String> temporalReplacements = new HashMap<>();
        Map<String, String> finalReplacements = new HashMap<>();
        for (Map.Entry<String, String> requiredReplacement : requiredReplacements.entrySet()) {
            String temporalVariable = "";
            boolean variableAccepted = false;
            while (!variableAccepted) {
                String triedNewVariable = generateRandomUnboundVariable();
                // there may be no final query variable that equals this name or
                // of which the new variable is a substring
                variableAccepted = conflictingVariables
                        .stream()
                        .noneMatch(s -> s.equals(triedNewVariable) ||
                                s.contains(triedNewVariable));
                if (variableAccepted) {
                    temporalVariable = triedNewVariable;
                    conflictingVariables.add(triedNewVariable);
                }
            }

            // split up replacements
            temporalReplacements.put(
                    requiredReplacement.getKey(), temporalVariable);
            finalReplacements.put(
                    temporalVariable, requiredReplacement.getValue());
        }

        print("Temporal replacements: " + temporalReplacements);
        print("Final replacements: " + finalReplacements);

        // first do temporal replacements
        List<String> sortedTemporalReplacementKeys = temporalReplacements.keySet()
                .stream()
                .sorted((s1, s2) -> s1.contains(s2) ?
                        (s1.equals(s2) ? 0 : -1) :
                        (s2.contains(s1) ? 1 : s1.compareTo(s2)))
                .collect(Collectors.toList());
        print("Order of temporal replacements: " + sortedTemporalReplacementKeys);
        for (String key : sortedTemporalReplacementKeys) {
            finalQuery = finalQuery.replaceAll(
                    Pattern.quote(key), temporalReplacements.get(key));
        }
        print("Final query after temporal replacements: " + finalQuery);

        // then also do final replacements
        List<String> finalTemporalReplacementKeys = finalReplacements.keySet()
                .stream()
                .sorted((s1, s2) -> s1.contains(s2) ?
                        (s1.equals(s2) ? 0 : -1) :
                        (s2.contains(s1) ? 1 : s1.compareTo(s2)))
                .collect(Collectors.toList());
        print("Order of final replacements: " + finalTemporalReplacementKeys);
        for (String key : finalTemporalReplacementKeys) {
            finalQuery = finalQuery.replaceAll(
                    Pattern.quote(key), finalReplacements.get(key));
        }
        print("Final query after final replacements: " + finalQuery);
        print("======================================");

        return new MappedDivideQueryParserInput(
                input.getInputQueryLanguage(),
                input.getStreamWindows(),
                input.getStreamQuery(),
                input.getIntermediateQueries(),
                finalQuery,
                input.getSolutionModifier(),
                requiredReplacements);
    }

    private DivideQueryParserOutput parseDivideQueryFromSparqlQueries(CleanDivideQueryParserInput input)
            throws InvalidDivideQueryParserInputException {
        // validate stream query
        validateSparqlQuery(input.getStreamQuery(), "Stream");

        // parse stream query
        ParsedSparqlQuery parsedStreamQuery = parseSparqlQuery(input.getStreamQuery());
        print("PARSED STREAM QUERY: " + parsedStreamQuery.toString());

        // if final query of input is not present, and query form of stream query
        // is not CONSTRUCT, a new input should be constructed in order to properly
        // deal with this!
        if (input.getFinalQuery() == null &&
                parsedStreamQuery.getSplitSparqlQuery().getQueryForm() != QueryForm.CONSTRUCT) {
            String constructTemplate;
            String newStreamQuery;
            String newFinalQuery;

            if (parsedStreamQuery.getSplitSparqlQuery().getQueryForm() == QueryForm.SELECT) {
                // in case of a SELECT query, all variables occurring in the
                // SELECT clause should be transformed to a CONSTRUCT template
                // -> first parse SELECT clause
                List<String> selectVariables = parseSelectClause(
                        parsedStreamQuery.getSplitSparqlQuery().getResultPart());

                // only retain those that match the actual variable mapping, excluding
                // "(... AS ?...)" definitions -> only those should be mapped to CONSTRUCT template
                List<String> actualSelectVariables = selectVariables
                        .stream()
                        .filter(s -> UNBOUND_VARIABLES_PATTERN.matcher(s).matches())
                        .collect(Collectors.toList());

                // create CONSTRUCT template with random triple for each variable
                constructTemplate = actualSelectVariables
                        .stream()
                        .map(s -> String.format("%s <http://idlab.ugent.be/divide/tmp/property/%s> " +
                                    "<http://idlab.ugent.be/divide/tmp/property/%s> .",
                                    s, UUID.randomUUID(), UUID.randomUUID()))
                        .collect(Collectors.joining(" "));

                // create updated final SELECT query based on CONSTRUCT template and original input
                newFinalQuery = String.format("%s SELECT %s WHERE { %s }",
                        parsedStreamQuery.getSplitSparqlQuery().getPrefixPart(),
                        parsedStreamQuery.getSplitSparqlQuery().getResultPart(),
                        constructTemplate).trim();

            } else if (parsedStreamQuery.getSplitSparqlQuery().getQueryForm() == QueryForm.DESCRIBE) {
                // in case of a DESCRIBE query, all variables occurring in the
                // DESCRIBE clause should be transformed to a CONSTRUCT template
                // -> first parse DESCRIBE clause
                List<String> describeVariables = new ArrayList<>();
                Matcher m = UNBOUND_VARIABLES_PATTERN.matcher(
                        parsedStreamQuery.getSplitSparqlQuery().getResultPart());
                while (m.find()) {
                    describeVariables.add(m.group());
                }

                // create CONSTRUCT template with random triple for each variable
                constructTemplate = describeVariables
                        .stream()
                        .map(s -> String.format("%s <http://idlab.ugent.be/divide/tmp/property/%s> " +
                                        "<http://idlab.ugent.be/divide/tmp/property/%s> .",
                                s, UUID.randomUUID(), UUID.randomUUID()))
                        .collect(Collectors.joining(" "));

                // create updated final DESCRIBE query based on CONSTRUCT template and original input
                newFinalQuery = String.format("%s DESCRIBE %s WHERE { %s }",
                        parsedStreamQuery.getSplitSparqlQuery().getPrefixPart(),
                        parsedStreamQuery.getSplitSparqlQuery().getResultPart(),
                        constructTemplate).trim();

            } else { // QueryForm.ASK
                // in case of an ASK query, no variables occur in the result part
                // -> a random triple should be generated to link both queries
                constructTemplate = String.format(
                        "<http://idlab.ugent.be/divide/tmp/property/%s> " +
                                "<http://idlab.ugent.be/divide/tmp/property/%s> "+
                                "<http://idlab.ugent.be/divide/tmp/property/%s> .",
                        UUID.randomUUID(), UUID.randomUUID(),
                        UUID.randomUUID());

                // create updated final DESCRIBE query based on CONSTRUCT template and original input
                newFinalQuery = String.format("%s ASK WHERE { %s }",
                        parsedStreamQuery.getSplitSparqlQuery().getPrefixPart(),
                        constructTemplate).trim();
            }

            // create updated stream query based on CONSTRUCT template and original input
            newStreamQuery = String.format("%s\nCONSTRUCT\n{\n%s\n}\n%s\nWHERE {\n%s\n} %s",
                    parsedStreamQuery.getSplitSparqlQuery().getPrefixPart(),
                    constructTemplate,
                    parsedStreamQuery.getSplitSparqlQuery().getFromPart(),
                    parsedStreamQuery.getSplitSparqlQuery().getWherePart(),
                    parsedStreamQuery.getSplitSparqlQuery().getFinalPart()).trim();

            // create new parser input based on new stream & final queries, and copy other entries
            CleanDivideQueryParserInput newInput = new CleanDivideQueryParserInput(
                    input.getInputQueryLanguage(),
                    input.getStreamWindows(),
                    newStreamQuery,
                    new ArrayList<>(),
                    newFinalQuery,
                    input.getSolutionModifier(),
                    input.getVariableMapping());
            newInput.setUnboundVariables(input.getUnboundVariables());
            newInput.setFinalQueryVariableMapping(input.getFinalQueryVariableMapping());
            newInput.preprocess();

            // perform the parsing again for this adapted input
            return parseDivideQueryFromSparqlQueries(newInput);
        }

        // check if stream query has no final part
        if (parsedStreamQuery.getSplitSparqlQuery().getFinalPart() != null &&
                !parsedStreamQuery.getSplitSparqlQuery().getFinalPart().trim().isEmpty()) {
            throw new InvalidDivideQueryParserInputException(
                    "Input queries cannot contain any solution modifiers, since this" +
                            " cannot be preserved by DIVIDE (because individual" +
                            " instantiated queries are generated). Any solution modifier" +
                            " for the queries derived by DIVIDE can be defined as a" +
                            " separate input entry.");
        }

        // validate stream window definitions
        for (StreamWindow streamWindow : input.getStreamWindows()) {
            Matcher m = RSP_QL_WINDOW_PARAMETERS_PATTERN.matcher(
                    streamWindow.getWindowDefinition());
            if (!m.matches()) {
                throw new InvalidDivideQueryParserInputException(
                        String.format("Stream window with name '%s' contains invalid" +
                                " RSP-QL window definition", streamWindow.getStreamIri()));
            }
        }

        // retrieve the graph names used in the FROM clauses of this SPARQL query
        Pair<List<String>, String> inputGraphNamesResult = retrieveGraphNamesFromSparqlFromPart(
                parsedStreamQuery.getSplitSparqlQuery().getFromPart(),
                parsedStreamQuery.getPrefixes());
        List<String> inputGraphNames = inputGraphNamesResult.getLeft();

        // parse remainder of FROM clause: it can only contain default graph patterns
        String fromPartLeftover = inputGraphNamesResult.getRight();
        Matcher m = SPARQL_FROM_DEFAULT_GRAPH_PATTERN.matcher(fromPartLeftover);
        while (m.find()) {
            fromPartLeftover = fromPartLeftover.replace(m.group().trim(), "").trim();
        }
        if (!fromPartLeftover.trim().isEmpty()) {
            throw new InvalidDivideQueryParserInputException(
                    String.format("SPARQL query contains invalid part '%s'", fromPartLeftover));
        }

        // parse the WHERE clause based on the used prefixes & defined input graph names
        WhereClause streamQueryWhereClause = parseWhereClauseOfQuery(
                parsedStreamQuery.getSplitSparqlQuery().getWherePart(),
                parsedStreamQuery.getPrefixes(),
                inputGraphNames,
                InputQueryLanguage.SPARQL);
        print("STREAM QUERY WHERE CLAUSE: " + streamQueryWhereClause);

        // parse where clause of stream query
        ParsedStreamQueryWhereClause parsedStreamQueryWhereClause =
                parseStreamQueryWhereClauseOfQuery(
                        streamQueryWhereClause,
                        input.getStreamWindows()
                                .stream()
                                .map(StreamWindow::getStreamIri)
                                .collect(Collectors.toList()));

        // validate parsed where clause of stream query: there should be at least
        // 1 graph on a stream IRI (otherwise there is no point of constructing
        // RSP queries with DIVIDE)
        if (parsedStreamQueryWhereClause.getStreamItems()
                .stream()
                .noneMatch(whereClauseItem
                        -> whereClauseItem.getItemType() == WhereClauseItemType.GRAPH)) {
            throw new InvalidDivideQueryParserInputException(
                    "Stream query should at least contain 1 graph on stream IRI in WHERE clause");
        }

        // validate defined solution modifier as valid SPARQL
        List<String> solutionModifierVariables = new ArrayList<>();
        if (!input.getSolutionModifier().trim().isEmpty()) {
            solutionModifierVariables.addAll(
                    findUnboundVariables(input.getSolutionModifier()));
            try {
                List<String> selectVariables = new ArrayList<>();
                List<String> whereClauseVariables = new ArrayList<>();
                Matcher solutionModifierMatcher =
                        GROUP_BY_PATTERN.matcher(input.getSolutionModifier());
                if (solutionModifierMatcher.find()) {
                    selectVariables.addAll(findUnboundVariables(solutionModifierMatcher.group()));
                    whereClauseVariables.addAll(solutionModifierVariables);
                } else {
                    if (solutionModifierVariables.isEmpty()) {
                        selectVariables.add("?x");
                    } else {
                        selectVariables.addAll(solutionModifierVariables);
                    }
                    whereClauseVariables.addAll(selectVariables);
                }
                String testQuery = String.format("SELECT %s WHERE { %s } %s",
                        String.join(" ", selectVariables),
                        whereClauseVariables.stream().map(s -> s + " ?a ?b . ").
                                collect(Collectors.joining(" ")),
                        input.getSolutionModifier());
                QueryFactory.create(testQuery);
            } catch (QueryParseException e) {
                throw new InvalidDivideQueryParserInputException(
                        "Defined solution modifier is no valid SPARQL");
            }
        }

        // validate variables used in stream window definitions
        // -> first parse to check if they should be mapped to a new variable
        //    based on the preprocessing
        // -> then check if antecedent of sensor query rule will contain this variable,
        //    OR that a default value is specified for this variable in the config
        List<ParsedStreamWindow> parsedStreamWindows = new ArrayList<>();
        for (StreamWindow streamWindow : input.getStreamWindows()) {
            ParsedStreamWindow parsedStreamWindow =
                    parseStreamWindow(streamWindow, input.getVariableMapping());

            List<String> unboundVariablesInContext =
                    findUnboundVariables(parsedStreamQueryWhereClause.getContextPart());
            for (String unboundVariable : parsedStreamWindow.getUnboundVariables()) {
                if (parsedStreamWindow.getDefaultWindowParameterValues().containsKey(unboundVariable)) {
                    if (unboundVariablesInContext.contains(unboundVariable)) {
                        throw new InvalidDivideQueryParserInputException(String.format(
                                "Variables defined in the stream window parameters should either occur " +
                                        "in the context part of the stream query (in order to be able " +
                                        "to be substituted during the query derivation), OR a default " +
                                        "value for this variable should be specified in the " +
                                        "configuration. For variable %s, the first condition is " +
                                        "fulfilled, so a default value cannot be specified in the " +
                                        "configuration.", input.getReverseVariableMapping().getOrDefault(
                                                unboundVariable, unboundVariable)));
                    }
                } else {
                    if (!unboundVariablesInContext.contains(unboundVariable)) {
                        throw new InvalidDivideQueryParserInputException(String.format(
                                "Variables defined in the stream window parameters should either occur " +
                                        "in the context part of the stream query (in order to be able " +
                                        "to be substituted during the query derivation), OR a default " +
                                        "value for this variable should be specified in the " +
                                        "configuration. For variable %s, the first condition is not " +
                                        "fulfilled, so a default value should be specified in the " +
                                        "configuration.", input.getReverseVariableMapping().getOrDefault(
                                                unboundVariable, unboundVariable)));
                    }
                }
            }

            parsedStreamWindows.add(parsedStreamWindow);
        }

        // declare variables which need to be initialized differently
        // based on the queries in the parser input
        String resultingQueryOutput;
        QueryForm resultingQueryForm;
        String goal;
        List<ParsedSparqlQuery> intermediateQueries = new ArrayList<>();
        Set<Prefix> queryPatternPrefixes;
        Set<Prefix> sensorQueryRulePrefixes;

        // if no final query is present, the streaming query is the only input
        // (there can also be no intermediate queries without a final query)
        if (input.getFinalQuery() == null) {
            // you already know it is a CONSTRUCT query, otherwise it will have been
            // transformed to a new input above

            // in that case, the original output of the streaming query is also
            // the output of the RSP-QL query generated with DIVIDE
            // (and similarly for the form of this query)
            resultingQueryOutput = parsedStreamQuery.getSplitSparqlQuery().getResultPart();
            resultingQueryForm = parsedStreamQuery.getSplitSparqlQuery().getQueryForm();

            // in this case, the query pattern prefixes can simply be the prefixes used
            // in the streaming query & sensor query rule
            queryPatternPrefixes = new HashSet<>(parsedStreamQuery.getPrefixes());
            sensorQueryRulePrefixes = new HashSet<>(parsedStreamQuery.getPrefixes());

            // in this case, the reasoner goal for DIVIDE is simply this query output
            // in both antecedent & consequence
            goal = divideQueryGenerator.createGoal(
                    parsedStreamQuery.getPrefixes(),
                    resultingQueryOutput,
                    resultingQueryOutput);

        } else {
            // if a final query is present, it should be ensured that the stream query
            // is of CONSTRUCT form (only the final query can have another form)
            if (parsedStreamQuery.getSplitSparqlQuery().getQueryForm() != QueryForm.CONSTRUCT) {
                throw new InvalidDivideQueryParserInputException(
                        "Stream query should be a CONSTRUCT query if another " +
                                "final query is specified");
            }

            // parse final query
            ParsedSparqlQuery parsedFinalQuery = parseSparqlQuery(input.getFinalQuery());

            // check if WHERE clause exists
            if (parsedFinalQuery.getSplitSparqlQuery().getWherePart() == null ||
                    parsedFinalQuery.getSplitSparqlQuery().getWherePart().trim().isEmpty()) {
                throw new InvalidDivideQueryParserInputException(
                        String.format("Final query of %s form should have a non-empty " +
                                "WHERE clause.%s", parsedFinalQuery.getSplitSparqlQuery().getQueryForm(),
                                parsedFinalQuery.getSplitSparqlQuery().getQueryForm() == QueryForm.ASK ?
                                        " For a final ASK query, the WHERE keyword should be " +
                                                "explicitly mentioned." : ""));
            }

            // check if result part is empty for ASK queries (= required!)
            if (parsedFinalQuery.getSplitSparqlQuery().getQueryForm() == QueryForm.ASK &&
                    parsedFinalQuery.getSplitSparqlQuery().getResultPart() != null &&
                    !parsedFinalQuery.getSplitSparqlQuery().getResultPart().trim().isEmpty()) {
                throw new InvalidDivideQueryParserInputException(
                        "Final query of ASK form should fulfill regex 'ASK (FROM .*)* WHERE {.*}'.");
            }

            // the final query may not contain any FROM definitions
            if (parsedFinalQuery.getSplitSparqlQuery().getFromPart() != null &&
                    !parsedFinalQuery.getSplitSparqlQuery().getFromPart().trim().isEmpty()) {
                throw new InvalidDivideQueryParserInputException(
                        "Final query cannot contain any FROM parts");
            }

            // check if final query has no final part
            if (parsedFinalQuery.getSplitSparqlQuery().getFinalPart() != null &&
                    !parsedFinalQuery.getSplitSparqlQuery().getFinalPart().trim().isEmpty()) {
                throw new InvalidDivideQueryParserInputException(
                        "Input queries cannot contain any solution modifiers, since this" +
                                " cannot be preserved by DIVIDE (because individual" +
                                " instantiated queries are generated). Any solution modifier" +
                                " for the queries derived by DIVIDE can be defined as a" +
                                " separate input entry.");
            }

            // ensure no conflicts exist between parsed final query & prefixes in stream query
            parsedFinalQuery = solvePrefixConflicts(
                    parsedStreamQuery.getPrefixes(), parsedFinalQuery);

            // in this case, the original output of the final query is also
            // the output of the RSP-QL query generated with DIVIDE
            // (and similarly for the form of this query)
            resultingQueryOutput = parsedFinalQuery.getSplitSparqlQuery().getResultPart();
            resultingQueryForm = parsedFinalQuery.getSplitSparqlQuery().getQueryForm();

            // in this case, the prefixes of both the stream & final query need
            // to be merged to be used for the query pattern
            queryPatternPrefixes = new HashSet<>(parsedStreamQuery.getPrefixes());
            queryPatternPrefixes.addAll(parsedFinalQuery.getPrefixes());

            // the sensor query rule prefixes set should only contain the prefixes
            // of the stream query (potentially added later with the prefixes of
            // any intermediate queries)
            sensorQueryRulePrefixes = new HashSet<>(parsedStreamQuery.getPrefixes());

            // in this case, the goal is constructed differently based on the query form:
            if (resultingQueryForm == QueryForm.CONSTRUCT) {
                // in case of a CONSTRUCT query, the goal takes the WHERE clause of
                // the final query as antecedent, and the output of the CONSTRUCT
                // query (i.e., the CONSTRUCT clause) as consequence
                goal = divideQueryGenerator.createGoal(
                        parsedFinalQuery.getPrefixes(),
                        parsedFinalQuery.getSplitSparqlQuery().getWherePart(),
                        resultingQueryOutput);

            } else { // QueryForm.ASK, QueryForm.DESCRIBE or QueryForm.SELECT
                // in case of a SELECT, ASK or DESCRIBE query, both the antecedent and
                // the consequence of the goal are set to the WHERE clause of the final query
                goal = divideQueryGenerator.createGoal(
                        parsedFinalQuery.getPrefixes(),
                        parsedFinalQuery.getSplitSparqlQuery().getWherePart(),
                        parsedFinalQuery.getSplitSparqlQuery().getWherePart());
            }

            // if intermediate queries are provided, they should be parsed and
            // added to the inputs for the creation of the sensor query rule
            if (!input.getIntermediateQueries().isEmpty()) {
                for (String intermediateQueryString : input.getIntermediateQueries()) {
                    // split intermediate query
                    ParsedSparqlQuery parsedIntermediateQuery =
                            parseSparqlQuery(intermediateQueryString);

                    // check if intermediate query has no final part
                    if (parsedIntermediateQuery.getSplitSparqlQuery().getFinalPart() != null &&
                            !parsedIntermediateQuery.getSplitSparqlQuery().getFinalPart().trim().isEmpty()) {
                        throw new InvalidDivideQueryParserInputException(
                                "Input queries cannot contain any solution modifiers, since this" +
                                        " cannot be preserved by DIVIDE (because individual" +
                                        " instantiated queries are generated). Any solution modifier" +
                                        " for the queries derived by DIVIDE can be defined as a" +
                                        " separate input entry.");
                    }

                    // ensure no conflicts exist between parsed intermediate query & prefixes
                    // in stream & final query
                    parsedIntermediateQuery = solvePrefixConflicts(
                            queryPatternPrefixes, parsedIntermediateQuery);

                    // add prefixes to prefixes used for sensor query rule
                    sensorQueryRulePrefixes.addAll(parsedIntermediateQuery.getPrefixes());

                    // ensure that intermediate query is of CONSTRUCT form (only the final query
                    // can have another form)
                    if (parsedIntermediateQuery.getSplitSparqlQuery().getQueryForm()
                            != QueryForm.CONSTRUCT) {
                        throw new InvalidDivideQueryParserInputException(
                                "Intermediate queries should always be CONSTRUCT queries");
                    }

                    // save intermediate query
                    intermediateQueries.add(parsedIntermediateQuery);
                }
            }
        }

        // convert the parsed stream windows into a set of converted stream windows
        List<ConvertedStreamWindow> convertedStreamWindows = new ArrayList<>();
        for (ParsedStreamWindow parsedStreamWindow : parsedStreamWindows) {
            convertedStreamWindows.add(convertParsedStreamWindow(parsedStreamWindow));
        }

        // generate RSP-QL query based on parsing output
        RspQlQueryBody rspQlQueryBody = divideQueryGenerator.createRspQlQueryBody(
                resultingQueryForm,
                resultingQueryOutput,
                parsedStreamQueryWhereClause.getStreamItems(),
                input.getSolutionModifier(),
                convertedStreamWindows,
                this);

        // retrieve input variables for sensor query rule
        List<String> inputVariables = retrieveInputVariables(
                parsedStreamQueryWhereClause.getContextPart(),
                rspQlQueryBody.getUnboundVariables());

        // check unbound variables of generated RSP-QL query body
        if (validateUnboundVariablesInRspQlQueryBody) {
            validateUnboundVariablesInRspQlQueryBody(
                    rspQlQueryBody, inputVariables, input.getReverseVariableMapping(),
                    input.getFinalQueryVariableMapping());
        }

        // check that solution modifier does not contain an input variable
        if (inputVariables.stream().anyMatch(
                solutionModifierVariables::contains)) {
            throw new InvalidDivideQueryParserInputException(
                    "Solution modifier contains variable that will be instantiated " +
                            "by the DIVIDE query derivation");
        }

        // check that solution modifier only contains variables that are occurring
        // in the RSP-QL query body
        if (!new HashSet<>(findUnboundVariables(rspQlQueryBody.getQueryBody().replace(
                input.getSolutionModifier(), "")))
                .containsAll(solutionModifierVariables)) {
            throw new InvalidDivideQueryParserInputException(
                    "Solution modifier contains variables that do not occur in the " +
                            "instantiated RSP-QL query body");
        }

        // save some variables that might or might not be updated below
        String sensorQueryRuleContextPart = parsedStreamQueryWhereClause.getContextPart();
        List<WhereClauseItem> parsedStreamQueryWhereClauseStreamItems =
                parsedStreamQueryWhereClause.getStreamItems();
        String parsedStreamQueryResultPart =
                parsedStreamQuery.getSplitSparqlQuery().getResultPart();

        // check to update RSP-QL body string for SELECT queries
        if (resultingQueryForm == QueryForm.SELECT) {
            // retrieve SELECT variables from output
            List<String> selectVariables = parseSelectClause(resultingQueryOutput);

            // adaptations are only needed if any of the select variables is an input
            // variable of the sensor query rule (because then it will be substituted)
            List<String> selectInputVariables = selectVariables
                    .stream()
                    .filter(inputVariables::contains)
                    .collect(Collectors.toList());
            if (!selectInputVariables.isEmpty()) {
                // calculate all input variables in the DIVIDE parser input
                Set<String> allInputVariables = input.getUnboundVariables();

                // generate a random DIVIDE variable for all SELECT input variables
                Map<String, String> variableMapping = new HashMap<>();
                for (String selectInputVariable : selectInputVariables) {
                    String newVariable = null;
                    boolean variableAccepted = false;
                    while (!variableAccepted) {
                        String triedNewVariable = generateRandomUnboundVariable();
                        // there may be no existing input variable that is contained in this
                        // possible new input variable
                        variableAccepted = allInputVariables
                                .stream()
                                .noneMatch(triedNewVariable::contains);
                        if (variableAccepted) {
                            newVariable = triedNewVariable;
                        }
                    }
                    variableMapping.put(selectInputVariable, newVariable);
                }

                // update list of input variables
                inputVariables = inputVariables
                        .stream()
                        .map(s -> variableMapping.getOrDefault(s, s))
                        .collect(Collectors.toList());

                // update sensor query rule context & consequence
                for (String selectInputVariable : selectInputVariables) {
                    sensorQueryRuleContextPart = sensorQueryRuleContextPart.replace(
                            selectInputVariable, variableMapping.get(selectInputVariable));
                    parsedStreamQueryResultPart = parsedStreamQueryResultPart.replace(
                            selectInputVariable, variableMapping.get(selectInputVariable));
                }

                // update stream windows
                List<ConvertedStreamWindow> newConvertedStreamWindows = new ArrayList<>();
                for (ConvertedStreamWindow convertedStreamWindow : convertedStreamWindows) {
                    String iri = convertedStreamWindow.getStreamIri();
                    String windowDefinition = convertedStreamWindow.getWindowDefinition();
                    for (String selectInputVariable : selectInputVariables) {
                        windowDefinition = windowDefinition.replaceAll(
                                Pattern.quote(String.format("?{%s}", selectInputVariable.substring(1))),
                                String.format("?{%s}", variableMapping.get(selectInputVariable).substring(1)));
                    }
                    List<WindowParameter> windowParameters = convertedStreamWindow.getWindowParameters();
                    windowParameters = windowParameters
                            .stream()
                            .map(wp -> new WindowParameter(
                                    variableMapping.getOrDefault(wp.getVariable(), wp.getVariable()),
                                    wp.isValueSubstitutionVariable() ?
                                            variableMapping.getOrDefault(wp.getVariable(), wp.getVariable()) :
                                            wp.getValue(),
                                    wp.getType(),
                                    wp.isValueSubstitutionVariable()))
                            .collect(Collectors.toList());
                    newConvertedStreamWindows.add(
                            new ConvertedStreamWindow(iri, windowDefinition, windowParameters));
                }
                convertedStreamWindows = new ArrayList<>(newConvertedStreamWindows);

                // update RSP-QL query body
                String solutionModifier = input.getSolutionModifier();
                for (String selectInputVariable : selectInputVariables) {
                    solutionModifier = solutionModifier.replace(
                            selectInputVariable, variableMapping.get(selectInputVariable));
                }
                resultingQueryOutput = selectVariables
                        .stream()
                        .map(s -> variableMapping.containsKey(s)
                                ? String.format("(%s AS %s)", variableMapping.get(s), s)
                                : s)
                        .collect(Collectors.joining(" "));
                List<WhereClauseItem> whereClauseStreamItems = new ArrayList<>();
                for (WhereClauseItem item : parsedStreamQueryWhereClauseStreamItems) {
                    if (item.getItemType() == WhereClauseItemType.EXPRESSION) {
                        WhereClauseExpressionItem expressionItem = (WhereClauseExpressionItem) item;
                        String expression = expressionItem.getExpression();
                        for (String selectInputVariable : selectInputVariables) {
                            expression = expression.replace(
                                    selectInputVariable, variableMapping.get(selectInputVariable));
                        }
                        whereClauseStreamItems.add(new WhereClauseExpressionItem(expression));

                    } else if (item.getItemType() == WhereClauseItemType.GRAPH) {
                        WhereClauseGraphItem graphItem = (WhereClauseGraphItem) item;
                        Graph graph = graphItem.getGraph();
                        String expression = graph.getClause();
                        for (String selectInputVariable : selectInputVariables) {
                            expression = expression.replace(
                                    selectInputVariable, variableMapping.get(selectInputVariable));
                        }
                        whereClauseStreamItems.add(new WhereClauseGraphItem(
                                new Graph(graph.getName(), expression)));
                    }
                }
                parsedStreamQueryWhereClauseStreamItems = new ArrayList<>(whereClauseStreamItems);
                rspQlQueryBody = divideQueryGenerator.createRspQlQueryBody(
                        resultingQueryForm,
                        resultingQueryOutput,
                        whereClauseStreamItems,
                        solutionModifier,
                        convertedStreamWindows,
                        this);
            }
        }

        // update output to be used for sensor query
        String sensorQueryRuleResult = extendOutputOfStreamQueryForSensorQueryRule(
                parsedStreamQueryWhereClauseStreamItems,
                parsedStreamQueryResultPart,
                sensorQueryRulePrefixes);

        // generate query pattern based on RSP-QL query body and parsing output
        String queryPattern = divideQueryGenerator.createQueryPattern(
                resultingQueryForm,
                queryPatternPrefixes,
                rspQlQueryBody.getQueryBody());

        // retrieve output variables for sensor query rule
        List<String> outputVariables = retrieveOutputVariables(
                sensorQueryRuleContextPart,
                sensorQueryRuleResult);

        // generate sensor query rule
        List<WindowParameter> allWindowParameters = new ArrayList<>();
        for (ConvertedStreamWindow convertedStreamWindow : convertedStreamWindows) {
            allWindowParameters.addAll(convertedStreamWindow.getWindowParameters());
        }
        String sensorQueryRule = divideQueryGenerator.createSensorQueryRule(
                sensorQueryRulePrefixes,
                sensorQueryRuleContextPart,
                sensorQueryRuleResult,
                inputVariables,
                allWindowParameters,
                outputVariables,
                intermediateQueries);

        return new DivideQueryParserOutput(
                queryPattern, sensorQueryRule, goal, resultingQueryForm);
    }

    private DivideQueryParserOutput parseDivideQueryFromRspQlQuery(CleanDivideQueryParserInput input)
            throws InvalidDivideQueryParserInputException {
        // only the main stream query should be considered in this case
        // window parameters are taken from the query

        // parse the RSP-QL stream query
        ParsedSparqlQuery parsedStreamQuery = parseRspQlQuery(input.getStreamQuery());

        // check if stream query has no final part
        if (parsedStreamQuery.getSplitSparqlQuery().getFinalPart() != null &&
                !parsedStreamQuery.getSplitSparqlQuery().getFinalPart().trim().isEmpty()) {
            throw new InvalidDivideQueryParserInputException(
                    "Input queries cannot contain any solution modifiers, since this" +
                            " cannot be preserved by DIVIDE (because individual" +
                            " instantiated queries are generated). Any solution modifier" +
                            " for the queries derived by DIVIDE can be defined as a" +
                            " separate input entry.");
        }

        // remove any specified default graph from the SPARQL query
        String streamQueryFromPart = parsedStreamQuery.getSplitSparqlQuery().getFromPart();
        Matcher m = RSP_QL_FROM_DEFAULT_GRAPH_PATTERN.matcher(streamQueryFromPart);
        while (m.find()) {
            streamQueryFromPart = streamQueryFromPart.replace(m.group().trim(), "");
        }

        // retrieve the graph names & stream windows used in the FROM clauses of this SPARQL query
        Pair<List<String>, String> inputGraphNamesResult = retrieveGraphNamesFromRspQlFromPart(
                streamQueryFromPart,
                parsedStreamQuery.getPrefixes());
        List<String> inputGraphNames = inputGraphNamesResult.getLeft();
        String streamQueryFromPartLeftover = inputGraphNamesResult.getRight();
        Map<String, StreamWindow> streamWindowMap =
                completeStreamWindowsFromRspQlFromPart(
                        input.getStreamWindows(),
                        streamQueryFromPartLeftover,
                        parsedStreamQuery.getPrefixes());
        inputGraphNames.addAll(streamWindowMap.keySet());

        // only allow CONSTRUCT RSP-QL queries
        // -> if they are of other form, they are translated to SPARQL and
        //    further parsed as if they were a SPARQL query
        if (parsedStreamQuery.getSplitSparqlQuery().getQueryForm() != QueryForm.CONSTRUCT) {
            // create SPARQL FROM part
            StringBuilder sparqlFromPart = new StringBuilder();
            for (String inputGraphName : inputGraphNames) {
                sparqlFromPart.append(String.format("FROM NAMED %s ",
                        streamWindowMap.containsKey(inputGraphName)
                                ? streamWindowMap.get(inputGraphName).getStreamIri()
                                : inputGraphName));
            }

            // create SPARQL WHERE clause
            Matcher m1 = Pattern.compile("WINDOW\\s+(\\S+)").matcher(
                    parsedStreamQuery.getSplitSparqlQuery().getWherePart());
            String sparqlWhereClause = parsedStreamQuery.getSplitSparqlQuery().getWherePart();
            while (m1.find()) {
                sparqlWhereClause = sparqlWhereClause.replaceFirst(
                        m1.group(),
                        String.format("GRAPH %s", streamWindowMap.get(
                                resolveGraphName(m1.group(1),
                                        parsedStreamQuery.getPrefixes())).getStreamIri()));
            }

            // translate RSP-QL stream query to SPARQL
            String sparqlStreamQuery = String.format("%s %s %s %s WHERE { %s } %s",
                    parsedStreamQuery.getSplitSparqlQuery().getPrefixPart(),
                    parsedStreamQuery.getSplitSparqlQuery().getQueryForm().toString(),
                    parsedStreamQuery.getSplitSparqlQuery().getResultPart(),
                    sparqlFromPart,
                    sparqlWhereClause,
                    parsedStreamQuery.getSplitSparqlQuery().getFinalPart());

            // construct new SPARQL input
            CleanDivideQueryParserInput newInput = new CleanDivideQueryParserInput(
                    InputQueryLanguage.SPARQL,
                    new ArrayList<>(streamWindowMap.values()),
                    sparqlStreamQuery,
                    new ArrayList<>(),
                    null,
                    input.getSolutionModifier(),
                    input.getVariableMapping());
            newInput.setUnboundVariables(input.getUnboundVariables());
            newInput.setFinalQueryVariableMapping(input.getFinalQueryVariableMapping());
            newInput.preprocess();

            print("RSP-QL query has no CONSTRUCT form => converted to SPARQL " +
                    "=> new input:\n" + newInput);

            return parseDivideQueryFromSparqlQueries(newInput);
        }

        // parse the WHERE clause based on the used prefixes & defined input graph names
        WhereClause streamQueryWhereClause = parseWhereClauseOfQuery(
                parsedStreamQuery.getSplitSparqlQuery().getWherePart(),
                parsedStreamQuery.getPrefixes(),
                inputGraphNames,
                InputQueryLanguage.RSP_QL);

        // validate stream query
        validateSparqlQuery(String.format("%s CONSTRUCT { %s } WHERE { %s }",
                parsedStreamQuery.getPrefixes()
                        .stream()
                        .map(prefix -> String.format("PREFIX %s %s",
                                prefix.getName(), prefix.getUri()))
                        .collect(Collectors.joining(" ")),
                parsedStreamQuery.getSplitSparqlQuery().getResultPart(),
                streamQueryWhereClause.getItems()
                        .stream()
                        .map(WhereClauseItem::getClause)
                        .collect(Collectors.joining(" "))),
                "Stream");

        // loop over WHERE clause items and adapt graph expression items:
        // use actual graph name instead of window name
        List<WhereClauseItem> newStreamQueryWhereClauseItems = new ArrayList<>();
        for (WhereClauseItem item : streamQueryWhereClause.getItems()) {
            if (item.getItemType() == WhereClauseItemType.GRAPH) {
                WhereClauseGraphItem graphItem = (WhereClauseGraphItem) item;
                if (streamWindowMap.containsKey(graphItem.getGraph().getName())) {
                    newStreamQueryWhereClauseItems.add(new WhereClauseGraphItem(
                            new Graph(streamWindowMap.get(graphItem.getGraph().getName()).getStreamIri(),
                                    graphItem.getGraph().getClause())));
                } else {
                    newStreamQueryWhereClauseItems.add(item);
                }
            } else {
                newStreamQueryWhereClauseItems.add(item);
            }
        }
        streamQueryWhereClause = new WhereClause(newStreamQueryWhereClauseItems);

        // parse where clause of stream query
        ParsedStreamQueryWhereClause parsedStreamQueryWhereClause =
                parseStreamQueryWhereClauseOfQuery(
                        streamQueryWhereClause,
                        streamWindowMap.values()
                                .stream()
                                .map(StreamWindow::getStreamIri)
                                .collect(Collectors.toList()));

        // validate parsed where clause of stream query: there should be at least
        // 1 graph on a stream IRI (otherwise there is no point of constructing
        // RSP queries with DIVIDE)
        if (parsedStreamQueryWhereClause.getStreamItems()
                .stream()
                .noneMatch(whereClauseItem
                        -> whereClauseItem.getItemType() == WhereClauseItemType.GRAPH)) {
            throw new InvalidDivideQueryParserInputException(
                    "Stream query should at least contain 1 graph on stream IRI in WHERE clause");
        }

        // validate defined solution modifier
        List<String> solutionModifierVariables = new ArrayList<>();
        if (!input.getSolutionModifier().trim().isEmpty()) {
            solutionModifierVariables.addAll(
                    findUnboundVariables(input.getSolutionModifier()));
            try {
                List<String> selectVariables = new ArrayList<>();
                List<String> whereClauseVariables = new ArrayList<>();
                Matcher solutionModifierMatcher =
                        GROUP_BY_PATTERN.matcher(input.getSolutionModifier());
                if (solutionModifierMatcher.find()) {
                    selectVariables.addAll(findUnboundVariables(solutionModifierMatcher.group()));
                    whereClauseVariables.addAll(solutionModifierVariables);
                } else {
                    if (solutionModifierVariables.isEmpty()) {
                        selectVariables.add("?x");
                    } else {
                        selectVariables.addAll(solutionModifierVariables);
                    }
                    whereClauseVariables.addAll(selectVariables);
                }
                String testQuery = String.format("SELECT %s WHERE { %s } %s",
                        String.join(" ", selectVariables),
                        whereClauseVariables.stream().map(s -> s + " ?a ?b . ").
                                collect(Collectors.joining(" ")),
                        input.getSolutionModifier());
                QueryFactory.create(testQuery);
            } catch (QueryParseException e) {
                throw new InvalidDivideQueryParserInputException(
                        "Defined solution modifier is no valid SPARQL");
            }
        }

        // validate variables used in stream window definitions
        // -> first parse to check if they should be mapped to a new variable
        //    based on the preprocessing
        // -> then check if antecedent of sensor query rule will contain this variable,
        //    OR that a default value is specified for this variable in the config
        List<ParsedStreamWindow> parsedStreamWindows = new ArrayList<>();
        for (StreamWindow streamWindow : streamWindowMap.values()) {
            ParsedStreamWindow parsedStreamWindow =
                    parseStreamWindow(streamWindow, input.getVariableMapping());

            List<String> unboundVariablesInContext =
                    findUnboundVariables(parsedStreamQueryWhereClause.getContextPart());
            for (String unboundVariable : parsedStreamWindow.getUnboundVariables()) {
                if (parsedStreamWindow.getDefaultWindowParameterValues().containsKey(unboundVariable)) {
                    if (unboundVariablesInContext.contains(unboundVariable)) {
                        throw new InvalidDivideQueryParserInputException(String.format(
                                "Variables defined in the stream window parameters should either occur " +
                                        "in the context part of the stream query (in order to be able " +
                                        "to be substituted during the query derivation), OR a default " +
                                        "value for this variable should be specified in the " +
                                        "configuration. For variable %s, the first condition is " +
                                        "fulfilled, so a default value cannot be specified in the " +
                                        "configuration.", input.getReverseVariableMapping().getOrDefault(
                                        unboundVariable, unboundVariable)));
                    }
                } else {
                    if (!unboundVariablesInContext.contains(unboundVariable)) {
                        throw new InvalidDivideQueryParserInputException(String.format(
                                "Variables defined in the stream window parameters should either occur " +
                                        "in the context part of the stream query (in order to be able " +
                                        "to be substituted during the query derivation), OR a default " +
                                        "value for this variable should be specified in the " +
                                        "configuration. For variable %s, the first condition is not " +
                                        "fulfilled, so a default value should be specified in the " +
                                        "configuration.", input.getReverseVariableMapping().getOrDefault(
                                        unboundVariable, unboundVariable)));
                    }
                }
            }

            parsedStreamWindows.add(parsedStreamWindow);
        }

        // declare variables which need to be initialized
        // based on the queries in the parser input
        String resultingQueryOutput;
        QueryForm resultingQueryForm;
        String goal;
        List<ParsedSparqlQuery> intermediateQueries = new ArrayList<>();
        Set<Prefix> queryPatternPrefixes;
        Set<Prefix> sensorQueryRulePrefixes;

        // the original output of the streaming query is also
        // the output of the RSP-QL query generated with DIVIDE
        // (and similarly for the form of this query)
        resultingQueryOutput = parsedStreamQuery.getSplitSparqlQuery().getResultPart();
        resultingQueryForm = parsedStreamQuery.getSplitSparqlQuery().getQueryForm();

        // in this case, the query pattern prefixes can simply be the prefixes used
        // in the streaming query & sensor query rule
        queryPatternPrefixes = new HashSet<>(parsedStreamQuery.getPrefixes());
        sensorQueryRulePrefixes = new HashSet<>(parsedStreamQuery.getPrefixes());

        // in this case, the reasoner goal for DIVIDE is simply this query output
        // in both antecedent & consequence
        goal = divideQueryGenerator.createGoal(
                parsedStreamQuery.getPrefixes(),
                resultingQueryOutput,
                resultingQueryOutput);

        // convert the parsed stream windows into a set of converted stream windows
        List<ConvertedStreamWindow> convertedStreamWindows = new ArrayList<>();
        for (ParsedStreamWindow parsedStreamWindow : parsedStreamWindows) {
            convertedStreamWindows.add(convertParsedStreamWindow(parsedStreamWindow));
        }

        // generate RSP-QL query based on parsing output
        RspQlQueryBody rspQlQueryBody = divideQueryGenerator.createRspQlQueryBody(
                resultingQueryForm,
                resultingQueryOutput,
                parsedStreamQueryWhereClause.getStreamItems(),
                input.getSolutionModifier(),
                convertedStreamWindows,
                this);

        // generate query pattern based on RSP-QL query body and parsing output
        // -> first, merge set of prefixes from
        String queryPattern = divideQueryGenerator.createQueryPattern(
                resultingQueryForm,
                queryPatternPrefixes,
                rspQlQueryBody.getQueryBody());

        // update output to be used for sensor query
        String sensorQueryResult = extendOutputOfStreamQueryForSensorQueryRule(
                parsedStreamQueryWhereClause.getStreamItems(),
                parsedStreamQuery.getSplitSparqlQuery().getResultPart(),
                sensorQueryRulePrefixes);

        // retrieve input and output variables for sensor query rule
        List<String> inputVariables = retrieveInputVariables(
                parsedStreamQueryWhereClause.getContextPart(),
                rspQlQueryBody.getUnboundVariables());
        List<String> outputVariables = retrieveOutputVariables(
                parsedStreamQueryWhereClause.getContextPart(),
                sensorQueryResult);

        // check unbound variables of generated RSP-QL query body
        if (validateUnboundVariablesInRspQlQueryBody) {
            validateUnboundVariablesInRspQlQueryBody(
                    rspQlQueryBody, inputVariables, input.getReverseVariableMapping(),
                    input.getFinalQueryVariableMapping());
        }

        // check that solution modifier does not contain an input variable
        if (inputVariables.stream().anyMatch(
                solutionModifierVariables::contains)) {
            throw new InvalidDivideQueryParserInputException(
                    "Solution modifier contains variable that will be instantiated " +
                            "by the DIVIDE query derivation");
        }

        // check that solution modifier only contains variables that are occurring
        // in the RSP-QL query body
        if (!new HashSet<>(findUnboundVariables(rspQlQueryBody.getQueryBody().replace(
                input.getSolutionModifier(), "")))
                .containsAll(solutionModifierVariables)) {
            throw new InvalidDivideQueryParserInputException(
                    "Solution modifier contains variables that do not occur in the " +
                            "instantiated RSP-QL query body");
        }

        // generate sensor query rule
        List<WindowParameter> allWindowParameters = new ArrayList<>();
        for (ConvertedStreamWindow convertedStreamWindow : convertedStreamWindows) {
            allWindowParameters.addAll(convertedStreamWindow.getWindowParameters());
        }
        String sensorQueryRule = divideQueryGenerator.createSensorQueryRule(
                sensorQueryRulePrefixes,
                parsedStreamQueryWhereClause.getContextPart(),
                sensorQueryResult,
                inputVariables,
                allWindowParameters,
                outputVariables,
                intermediateQueries);

        return new DivideQueryParserOutput(
                queryPattern, sensorQueryRule, goal, resultingQueryForm);
    }

    /**
     * Solves prefix conflicts in a given parsed SPARQL query.
     * To do so, the method checks whether any of the prefix names in the given
     * set of existing prefixes occurs somewhere in the SPARQL query. If this is
     * the case, the corresponding URI should be identical to the existing prefix.
     * If not, the parsed SPARQL query should be updated: a new prefix should be
     * created and used in the corresponding query parts.
     *
     * @param existingPrefixes set of prefixes to which the prefixes in the given
     *                         SPARQL query should be compared and solved in case
     *                         of conflicts
     * @param parsedSparqlQuery parsed SPARQL query which needs to be checked for
     *                          any prefix conflicts
     * @return new parsed SPARQL query in which all possible prefix conflicts
     *         are resolved
     */
    private ParsedSparqlQuery solvePrefixConflicts(Set<Prefix> existingPrefixes,
                                                   ParsedSparqlQuery parsedSparqlQuery) {
        // create a list of conflicting prefixes in the given SPARQL query
        Set<Prefix> conflictingPrefixes = new HashSet<>();
        for (Prefix prefix : parsedSparqlQuery.getPrefixes()) {
            for (Prefix existingPrefix : existingPrefixes) {
                // there is a conflict if a prefix of the given SPARQL query has the
                // same name as an existing prefix, but another URI
                if (existingPrefix.getName().equals(prefix.getName()) &&
                        !existingPrefix.getUri().equals(prefix.getUri())) {
                    conflictingPrefixes.add(prefix);
                    break;
                }
            }
        }

        // if there are no conflicts, the same parsed SPARQL query can be returned
        if (conflictingPrefixes.isEmpty()) {
            return parsedSparqlQuery;

        } else {
            // otherwise, the conflicting prefixes should be given a different name
            // -> start from current fields
            String prefixPart = parsedSparqlQuery.getSplitSparqlQuery().getPrefixPart();
            String resultPart = parsedSparqlQuery.getSplitSparqlQuery().getResultPart();
            String wherePart = parsedSparqlQuery.getSplitSparqlQuery().getWherePart();
            Set<Prefix> prefixes = new HashSet<>(parsedSparqlQuery.getPrefixes());

            for (Prefix conflictingPrefix : conflictingPrefixes) {
                String newPrefixName = null;
                Prefix newPrefix = null;

                // check if a prefix with the same URI already existed
                for (Prefix existingPrefix : existingPrefixes) {
                    if (existingPrefix.getUri().equals(conflictingPrefix.getUri())) {
                        // if so, this one can be reused!
                        newPrefixName = existingPrefix.getName();
                        newPrefix = existingPrefix;
                    }
                }
                // if not, create a new prefix with new name and same URI
                if (newPrefixName == null) {
                    newPrefixName = String.format("newPrefix%d:", PREFIX_COUNTER++);
                    newPrefix = new Prefix(newPrefixName, conflictingPrefix.getUri());
                }

                // replace prefix name in existing query parts
                Pattern replacingPattern =
                        Pattern.compile("(\\s|\\(|^|\\^)" + conflictingPrefix.getName());
                Matcher m1 = replacingPattern.matcher(prefixPart);
                prefixPart = m1.replaceAll("$1" + newPrefixName);
                if ((parsedSparqlQuery.getSplitSparqlQuery().getQueryForm() == QueryForm.CONSTRUCT ||
                        parsedSparqlQuery.getSplitSparqlQuery().getQueryForm() == QueryForm.DESCRIBE ||
                        parsedSparqlQuery.getSplitSparqlQuery().getQueryForm() == QueryForm.SELECT)
                        && resultPart != null) {
                    Matcher m2 = replacingPattern.matcher(resultPart);
                    resultPart = m2.replaceAll("$1" + newPrefixName);
                }
                if (wherePart != null) {
                    Matcher m3 = replacingPattern.matcher(wherePart);
                    wherePart = m3.replaceAll("$1" + newPrefixName);
                }

                // update set of prefixes
                prefixes.remove(conflictingPrefix);
                prefixes.add(newPrefix);
            }

            // return updated query
            return new ParsedSparqlQuery(
                    new SplitSparqlQuery(
                            prefixPart,
                            parsedSparqlQuery.getSplitSparqlQuery().getQueryForm(),
                            resultPart,
                            parsedSparqlQuery.getSplitSparqlQuery().getFromPart(),
                            wherePart,
                            parsedSparqlQuery.getSplitSparqlQuery().getFinalPart()),
                    prefixes);
        }
    }

    /**
     * Validates the occurrence of unbound variables in the RSP-QL query body generated
     * by this parser. If validation succeeds, this method returns after performing its
     * checks. If validation fails, a {@link InvalidDivideQueryParserInputException} is
     * thrown.
     * Validation fails if the RSP-QL query body contains variables in its result part
     * that are not occurring in the WHERE clause, and also not in the input variables
     * defined for substitution for the query derivation. If such variables exist, they
     * will lead to errors when registering a query.
     * Validation also fails if the RSP-QL query body is a SELECT query which contains
     * "... AS ?var" expressions, where ?var is already occurring as variable name in
     * the WHERE clause of the query OR in the list of input variables.
     *
     * @param rspQlQueryBody RSP-QL query body that is about to be validated
     * @param inputVariables input variables for the DIVIDE query derivation, that may
     *                       occur in the RSP-QL query body but will later be substituted
     * @param outputMapping mapping of variables in the parser input to the original input
     *                      variables (needs to be provided to give a clear error message
     *                      to the end user about the problematic variables, is not used
     *                      as such for the validation)
     * @throws InvalidDivideQueryParserInputException when validation fails
     */
    private void validateUnboundVariablesInRspQlQueryBody(RspQlQueryBody rspQlQueryBody,
                                                          List<String> inputVariables,
                                                          Map<String, String> outputMapping,
                                                          Map<String, String> finalQueryMapping)
            throws InvalidDivideQueryParserInputException {
        print("VALIDATING UNBOUND VARIABLES IN RSP-QL QUERY BODY");

        // obtain all unbound variables in WHERE clause
        Set<String> unboundVariablesInWherePart =
                new HashSet<>(findUnboundVariables(rspQlQueryBody.getWherePart()));

        // obtain all unbound variables in result part
        // -> special caveat required for SELECT queries
        Pair<Set<String>, Set<String>> unboundVariablesInResultPart =
                findUnboundVariablesInQueryResultPart(
                        rspQlQueryBody.getResultPart(), rspQlQueryBody.getQueryForm());
        Set<String> expectedUnboundVariablesInResultPart = unboundVariablesInResultPart.getLeft();
        Set<String> forbiddenUnboundVariablesInResultPart = unboundVariablesInResultPart.getRight();

        print("Output mapping: " + outputMapping);
        print("Final query variable mapping: " + finalQueryMapping);

        print("Mappings for expected unbound variables in result part " +
                "(var -> output mapping -> final query variable mapping)");
        for (String s : expectedUnboundVariablesInResultPart) {
            String m1 = outputMapping.getOrDefault(s, s);
            String m2 = finalQueryMapping.getOrDefault(m1, m1);
            print(String.format("  %s - %s - %s", s, m1, m2));
        }

        print("Forbidden variables: " + forbiddenUnboundVariablesInResultPart);
        print("WHERE clause: " + rspQlQueryBody.getWherePart());
        print("Unbound variables in WHERE clause: " + unboundVariablesInWherePart);
        // validate that none of the forbidden variables occurs in the WHERE clause
        List<String> problematicVariables = forbiddenUnboundVariablesInResultPart
                .stream()
                .filter(s -> unboundVariablesInWherePart.contains(s)
                        || inputVariables.contains(s)
                        || expectedUnboundVariablesInResultPart.contains(s))
                .map(s -> outputMapping.getOrDefault(s, s))
                .collect(Collectors.toList());
        if (!problematicVariables.isEmpty()) {
            throw new InvalidDivideQueryParserInputException(String.format(
                    "The SELECT clause of the resulting RSP-QL query body " +
                            "will contain template variables that are not allowed in the " +
                            "WHERE clause, but that are present there: %s. This is probably " +
                            "caused by an invalid SELECT clause in the stream or final query. " +
                            "Make sure this clause is valid. Also make sure that a correct " +
                            "mapping file is provided, and/or that variables with identical " +
                            "names should be mapped (if this is enabled via the settings).",
                    String.join(", ", problematicVariables)));
        }

        print("Validating unbound variables for: " + rspQlQueryBody.getQueryBody());
        print("Input variables at this point: " + inputVariables);
        // check if the result part of the RSP-QL query body does not contain any
        // invalid unbound variables
        // -> invalid means that they do not occur in the WHERE clause, and also not
        //    in the set of input variables that are about to be replaced
        problematicVariables = expectedUnboundVariablesInResultPart
                .stream()
                .filter(s -> !unboundVariablesInWherePart.contains(s)
                        && !inputVariables.contains(s))
                .map(s -> {
                    String m1 = outputMapping.getOrDefault(s, s);
                    return finalQueryMapping.getOrDefault(m1, m1);
                })
                .collect(Collectors.toList());
        if (!problematicVariables.isEmpty()) {
            throw new InvalidDivideQueryParserInputException(String.format(
                    "Resulting RSP-QL query body will contain invalid variables in result part, " +
                            "that are not present in WHERE clause and will also not be replaced " +
                            "during the DIVIDE query derivation: %s. Make sure the input is correct. " +
                            "If the input contains a final query, make sure to define a mapping of a " +
                            "variable in the stream query to each of these variable in the final query " +
                            "(or allow automatic mapping of matching variable names via the settings). " +
                            "If the input only contains a stream query, make sure the WHERE clause " +
                            "of the stream query contains these variables.",
                    String.join(", ", problematicVariables)));
        }

        print("======================================");
    }

    private Pair<Set<String>, Set<String>> findUnboundVariablesInQueryResultPart(String result,
                                                                                 QueryForm queryForm)
            throws InvalidDivideQueryParserInputException {
        print("-> FINDING UNBOUND VARIABLES IN QUERY RESULT PART");
        if (queryForm == QueryForm.SELECT) {
            Set<String> expectedVariables = new HashSet<>();
            Set<String> forbiddenVariables = new HashSet<>();
            String formattedSelectClause = String.format("%s ", result.trim());
            if (SELECT_CLAUSE_PATTERN.matcher(formattedSelectClause).matches()) {
                Matcher m = SELECT_CLAUSE_PATTERN_ENTRY.matcher(formattedSelectClause);
                while(m.find()) {
                    String match = m.group().trim();
                    Matcher m2 = SELECT_CLAUSE_EXPRESSION_PATTERN.matcher(match);
                    if (m2.matches()) {
                        print("     Expression pattern match: '" + match + "'");
                        // if it matches the expression "... AS ?...", then only the first part
                        // should be returned as a variable, IF it is a variable of course
                        m2.reset();
                        while (m2.find()) {
                            if (VAR1_PATTERN.matcher(m2.group(1)).matches()) {
                                expectedVariables.add(m2.group(1));
                                print("     Varname in expression: '" + m2.group(1) + "'");
                            } else {
                                print("     NO varname in expression: '" + m2.group(1) + "'");
                            }
                            forbiddenVariables.add(m2.group(2));
                            print("     Forbidden variable: " + m2.group(2));
                        }
                    } else {
                        print("     Varname match: '" + match + "'");
                        // if no match with expression, then this match is a single variable name
                        expectedVariables.add(match);
                    }
                }
                return Pair.create(expectedVariables, forbiddenVariables);

            } else {
                throw new InvalidDivideQueryParserInputException(
                        "SELECT clause of resulting RSP-QL query is invalid, which is probably " +
                                "caused by an invalid SELECT clause in the stream or final query.");
            }
        } else {
            return Pair.create(
                    new HashSet<>(findUnboundVariables(result)),
                    new HashSet<>());
        }
    }

    /**
     * @param query SPARQL query body string
     * @return parsed version of the given SPARQL query
     * @throws InvalidDivideQueryParserInputException if the query is of invalid syntax
     */
    @Override
    public ParsedSparqlQuery parseSparqlQuery(String query)
            throws InvalidDivideQueryParserInputException {
        // first split SPARQL query into its different parts
        SplitSparqlQuery splitSparqlQuery = splitSparqlQuery(query);

        // retrieve the prefixes used in this SPARQL query
        Set<Prefix> prefixes = getPrefixes(splitSparqlQuery.getPrefixPart());

        // check for conflicting prefixes
        Map<String, String> prefixMap = new HashMap<>();
        for (Prefix prefix : prefixes) {
            if (prefixMap.containsKey(prefix.getName()) &&
                    !prefixMap.get(prefix.getName()).equals(prefix.getUri())) {
                throw new InvalidDivideQueryParserInputException(
                        String.format("Multiple prefixes are present with name '%s'",
                                prefix.getName()));
            }
            prefixMap.put(prefix.getName(), prefix.getUri());
        }

        // check for prefix names occurring in query string without being defined as a prefix
        String queryWithoutPrefixes = query.replace(splitSparqlQuery.getPrefixPart(), "");
        Set<String> existingPrefixNames = prefixMap.keySet();
        Matcher m = USED_PREFIX_PATTERN.matcher(queryWithoutPrefixes);
        while (m.find()) {
            if (!existingPrefixNames.contains(m.group(2)) && !"_:".equals(m.group(2))) {
                throw new InvalidDivideQueryParserInputException(
                        String.format("Query string contains undefined prefix '%s'", m.group(2)));
            }
        }

        // remove prefixes that do not occur in query body
        prefixes.removeIf(prefix -> !Pattern.compile("(\\s|\\(|^|\\^)" + prefix.getName())
                .matcher(queryWithoutPrefixes).find());

        return new ParsedSparqlQuery(
                splitSparqlQuery,
                prefixes);
    }

    /**
     * @param query RSP-QL query body string
     * @return parsed version of the given RSP-QL query
     * @throws InvalidDivideQueryParserInputException if the query is of invalid syntax
     */
    private ParsedSparqlQuery parseRspQlQuery(String query)
            throws InvalidDivideQueryParserInputException {
        // first split RSP-QL query as a SPARQL query into its different parts
        SplitSparqlQuery splitSparqlQuery = splitSparqlQuery(query);

        // retrieve the prefixes used in this SPARQL query
        Set<Prefix> prefixes = getPrefixes(splitSparqlQuery.getPrefixPart());

        // check for conflicting prefixes
        Map<String, String> prefixMap = new HashMap<>();
        for (Prefix prefix : prefixes) {
            if (prefixMap.containsKey(prefix.getName()) &&
                    !prefixMap.get(prefix.getName()).equals(prefix.getUri())) {
                throw new InvalidDivideQueryParserInputException(
                        String.format("Multiple prefixes are present with name '%s'",
                                prefix.getName()));
            }
            prefixMap.put(prefix.getName(), prefix.getUri());
        }

        // check for prefix names occurring in query string without being defined as a prefix
        String queryWithoutPrefixes = query.replace(splitSparqlQuery.getPrefixPart(), "");
        Set<String> existingPrefixNames = new HashSet<>(prefixMap.keySet());
        Matcher m = USED_PREFIX_PATTERN.matcher(queryWithoutPrefixes);
        while (m.find()) {
            if (!existingPrefixNames.contains(m.group(2))) {
                if (":".equals(m.group(2))) {
                    String prefixUri = "<http://acrasycompany.org/rsp#>";
                    splitSparqlQuery = new SplitSparqlQuery(
                            splitSparqlQuery.getPrefixPart() + " PREFIX : " + prefixUri,
                            splitSparqlQuery.getQueryForm(),
                            splitSparqlQuery.getResultPart(),
                            splitSparqlQuery.getFromPart(),
                            splitSparqlQuery.getWherePart(),
                            splitSparqlQuery.getFinalPart());
                    existingPrefixNames.add(":");
                    prefixes.add(new Prefix(":", prefixUri));
                } else {
                    throw new InvalidDivideQueryParserInputException(
                            String.format("Query string contains undefined prefix '%s'", m.group(2)));
                }
            }
        }

        // remove prefixes that do not occur in query body
        prefixes.removeIf(prefix -> !Pattern.compile("(\\s|\\(|^|\\^)" + prefix.getName())
                .matcher(queryWithoutPrefixes).find());

        return new ParsedSparqlQuery(
                splitSparqlQuery,
                prefixes);
    }

    /**
     * @param query SPARQL query body string
     * @return split SPARQL query containing the different parts
     * @throws InvalidDivideQueryParserInputException if the query is of invalid syntax
     */
    private SplitSparqlQuery splitSparqlQuery(String query)
            throws InvalidDivideQueryParserInputException {
        // try to match the query pattern on the SPARQL query
        Matcher m = SPARQL_QUERY_SPLIT_PATTERN.matcher(query);
        if (m.find()) {
            // parse query form
            QueryForm queryForm = QueryForm.fromString(m.group(5).trim());
            if (queryForm == null) {
                throw new InvalidDivideQueryParserInputException(
                        "Invalid query form specified in query");
            }

            // parse result part (output) & remove curly braces
            String resultPart = m.group(6).trim();
            resultPart = parseQueryResultPart(resultPart, queryForm);

            // create split query & make sure all strings are trimmed
            return new SplitSparqlQuery(
                    m.group(1) == null ? null : m.group(1).trim(),
                    queryForm,
                    resultPart,
                    m.group(8) == null ? null : m.group(8).trim(),
                    m.group(12) == null ? null : m.group(12).trim(),
                    m.group(13) == null ? null : m.group(13).trim());

        } else {
            throw new InvalidDivideQueryParserInputException(
                    "Query does not have valid SPARQL format");
        }
    }

    /**
     * Parses query result part (output) & removes curly braces if present.
     *
     * @param resultPart result part string
     * @param queryForm form of query
     * @return parsed result part string
     * @throws InvalidDivideQueryParserInputException if result part is invalid
     */
    private String parseQueryResultPart(String resultPart, QueryForm queryForm)
            throws InvalidDivideQueryParserInputException {
        if (resultPart.startsWith("{")) {
            if (resultPart.endsWith("}")) {
                resultPart = resultPart.substring(1, resultPart.length() - 1).trim();
            } else {
                throw new InvalidDivideQueryParserInputException(
                        String.format("Format of %s clause is invalid", queryForm.toString()));
            }
        } else {
            if (resultPart.endsWith("}")) {
                throw new InvalidDivideQueryParserInputException(
                        String.format("Format of %s clause is invalid", queryForm.toString()));
            }
        }
        return resultPart;
    }

    /**
     * @param prefixString string of prefixes as defined in a SPARQL or RSP-QL query
     * @return parsed set of prefixes
     */
    private Set<Prefix> getPrefixes(String prefixString) {
        Matcher m = PREFIX_PATTERN.matcher(prefixString);
        Set<Prefix> prefixes = new HashSet<>();
        while (m.find()) {
            String prefixName = m.group(2).trim();
            String prefixURI = m.group(3).trim();
            prefixes.add(new Prefix(prefixName, prefixURI));
        }
        return prefixes;
    }

    /**
     * @param fromPart string with FROM clauses as defined in SPARQL query
     * @param prefixes set of prefixes to be used for resolving the graph names
     *                 occurring in the FROM clauses of the SPARQL query
     * @return pair with as left value the strings of actual graph names appearing
     *         in the FROM clauses, as right value the remainder of the FROM part
     *         with all matching named graphs removed
     * @throws InvalidDivideQueryParserInputException if any of the graph names
     *                                                occurring in the FROM clause
     *                                                is invalid
     */
    private Pair<List<String>, String> retrieveGraphNamesFromSparqlFromPart(String fromPart,
                                                                            Set<Prefix> prefixes)
            throws InvalidDivideQueryParserInputException {
        String fromPartLeftover = fromPart;
        Matcher matcher = SPARQL_FROM_NAMED_GRAPH_PATTERN.matcher(fromPart);
        List<String> graphNames = new ArrayList<>();
        while (matcher.find()) {
            graphNames.add(resolveGraphName(matcher.group(1), prefixes));
            fromPartLeftover = fromPartLeftover.replace(matcher.group().trim(), "").trim();
        }
        return Pair.create(graphNames, fromPartLeftover);
    }

    /**
     * @param fromPart string with FROM clauses as defined in RSP-QL query
     * @param prefixes set of prefixes to be used for resolving the graph names
     *                 occurring in the FROM clauses of the SPARQL query
     * @return pair with as left value the strings of actual graph names appearing
     *         in the FROM clauses, as right value the remainder of the FROM part
     *         with all matching named graphs removed
     * @throws InvalidDivideQueryParserInputException if any of the graph names
     *                                                occurring in the FROM clause
     *                                                is invalid
     */
    private Pair<List<String>, String> retrieveGraphNamesFromRspQlFromPart(String fromPart,
                                                                           Set<Prefix> prefixes)
            throws InvalidDivideQueryParserInputException {
        String fromPartLeftover = fromPart;
        Matcher matcher = RSP_QL_FROM_NAMED_GRAPH_PATTERN.matcher(fromPart);
        List<String> graphNames = new ArrayList<>();
        while (matcher.find()) {
            graphNames.add(resolveGraphName(matcher.group(1), prefixes));
            fromPartLeftover = fromPartLeftover.replace(matcher.group().trim(), "").trim();
        }
        return Pair.create(graphNames, fromPartLeftover);
    }

    /**
     * @param streamWindows list of possibly incomplete stream windows, which might not contain
     *                      the stream window definition
     * @param fromPart string with FROM clauses as defined in RSP-QL query
     * @return stream windows completed according to how they are appearing in these FROM clauses,
     *         associated in a map to a key representing the window name in the query
     * @throws InvalidDivideQueryParserInputException when a window name is defined more
     *                                                than once, or if any of the graph names
     *                                                occurring in the FROM clause is invalid
     */
    private Map<String, StreamWindow> completeStreamWindowsFromRspQlFromPart(List<StreamWindow> streamWindows,
                                                                             String fromPart,
                                                                             Set<Prefix> prefixes)
            throws InvalidDivideQueryParserInputException {
        String fromPartLeftover = fromPart;
        Matcher matcher = RSP_QL_FROM_NAMED_WINDOW_PATTERN.matcher(fromPart);
        Map<String, StreamWindow> streamWindowMap = new HashMap<>();
        while (matcher.find()) {
            String windowName = resolveGraphName(matcher.group(1), prefixes);
            if (streamWindowMap.containsKey(windowName)) {
                throw new InvalidDivideQueryParserInputException(
                        String.format("Window name '%s' defined more than once", windowName));
            }
            String streamName = resolveGraphName(matcher.group(2), prefixes);
            Matcher m2 = RSP_QL_WINDOW_PARAMETERS_PATTERN.matcher(matcher.group(3));
            if (m2.find()) {
                streamWindowMap.put(windowName, new StreamWindow(streamName,
                        String.format("%s %s", m2.group(1), m2.group(7))));
            } else {
                throw new InvalidDivideQueryParserInputException(
                        String.format("Window definition of stream '%s' is no " +
                                "valid RSP-QL", streamName));
            }
            fromPartLeftover = fromPartLeftover.replace(matcher.group().trim(), "").trim();
        }
        if (!fromPartLeftover.trim().isEmpty()) {
            throw new InvalidDivideQueryParserInputException(
                    String.format("RSP-QL query contains invalid part '%s'", fromPartLeftover));
        }

        // check if every stream window defined in the JSON config also occurs
        // in the RSP-QL from part, and append the default window parameters
        Collection<StreamWindow> rspQlStreamWindows = streamWindowMap.values();
        for (StreamWindow definedStreamWindow : streamWindows) {
            Optional<StreamWindow> matchingStreamWindow = rspQlStreamWindows.stream()
                    .filter(sw -> sw.getStreamIri().equals(definedStreamWindow.getStreamIri()))
                    .findFirst();
            if (matchingStreamWindow.isPresent()) {
                if (definedStreamWindow.getWindowDefinition() != null &&
                        !matchingStreamWindow.get().getWindowDefinition().equals(
                                definedStreamWindow.getWindowDefinition())) {
                    throw new InvalidDivideQueryParserInputException(String.format(
                            "Configuration contains stream window with IRI '%s' that has a different window " +
                                    "definition than the corresponding stream window present in the " +
                                    "RSP-QL stream query", definedStreamWindow.getStreamIri()));
                }
                matchingStreamWindow.get().setDefaultWindowParameterValues(
                        definedStreamWindow.getDefaultWindowParameterValues());
            } else {
                throw new InvalidDivideQueryParserInputException(String.format(
                        "Configuration contains stream window with IRI '%s' that does not occur " +
                                "in the RSP-QL stream query", definedStreamWindow.getStreamIri()));
            }
        }

        return streamWindowMap;
    }

    /**
     * @param whereClause extracted WHERE clause of a SPARQL or RSP-QL query
     * @param prefixes set of prefixes used in this query
     * @param inputGraphNames graph names specified in the FROM clauses of this query
     * @param queryLanguage language used for specifying the WHERE clause of this query
     *                      (this can either be SPARQL or RSP-QL)
     * @return parsed WHERE clause of the query, containing a list of WHERE clause
     *         items which can either be graphs or expressions
     * @throws InvalidDivideQueryParserInputException if the WHERE clause contains invalid
     *                                                graph names
     */
    private WhereClause parseWhereClauseOfQuery(String whereClause,
                                                Set<Prefix> prefixes,
                                                List<String> inputGraphNames,
                                                InputQueryLanguage queryLanguage)
            throws InvalidDivideQueryParserInputException {
        List<WhereClauseItem> items = new ArrayList<>();

        // pattern to be used for parsing depends on query language
        Pattern pattern;
        if (queryLanguage == InputQueryLanguage.SPARQL) {
            pattern = SPARQL_WHERE_CLAUSE_GRAPH_PATTERN;
        } else { // RSP_QL query language
            pattern = RSP_QL_WHERE_CLAUSE_GRAPH_OR_WINDOW_PATTERN;
        }

        // make sure WHERE clauses is trimmed before parsing
        whereClause = whereClause.trim();

        // try to find graph patterns in WHERE clauses
        Matcher matcher = pattern.matcher(whereClause);
        int lastEndIndex = 0;

        // loop over all found graph patterns
        while (matcher.find()) {
            int startIndex = matcher.start();
            int endIndex = matcher.end();

            // update indices & create expression item if some text is found in between
            // last match and this match
            if (startIndex != lastEndIndex) {
                String expression = whereClause.substring(lastEndIndex, startIndex).trim();
                items.add(new WhereClauseExpressionItem(expression));
            }

            // find end of graph pattern
            int braceCount = 1;
            int loopIndex = endIndex + 1;
            while (braceCount > 0) {
                char c = whereClause.charAt(loopIndex);
                if (c == '{') {
                    braceCount++;
                } else if (c == '}') {
                    braceCount--;
                }
                loopIndex++;
            }
            lastEndIndex = loopIndex;

            // parse name of found graph
            String name = matcher.group(2).trim();
            name = resolveGraphName(name, prefixes);
            if (!inputGraphNames.contains(name)) {
                throw new InvalidDivideQueryParserInputException(
                        String.format("Graph name '%s' not specified with FROM", name));
            }

            // parse clause of found graph
            String clause = whereClause.substring(endIndex + 1, loopIndex - 1).trim();

            // create graph item for the found graph with parsed name & clause
            items.add(new WhereClauseGraphItem(new Graph(name, clause)));
        }

        // process possible expression after last found graph
        // (if no graph is found, this expression will contain the full WHERE clause)
        if (lastEndIndex != whereClause.length()) {
            String lastExpression = whereClause.substring(lastEndIndex).trim();
            items.add(new WhereClauseExpressionItem(lastExpression));
        }

        return new WhereClause(items);
    }

    /**
     * Resolves a graph name against a set of prefixes.
     * If the graph name is not an IRI (<...>), then it should start with a
     * prefix in the specified list.
     *
     * @param graphName graph name to be resolved
     * @param prefixes set of prefixes to be used for resolving the graph name
     * @return resolved graph name (can be the same as the input if it was
     *         already a valid IRI)
     * @throws InvalidDivideQueryParserInputException if the graph name is invalid
     *                                                (invalid syntax or non-existing prefix)
     */
    private String resolveGraphName(String graphName, Set<Prefix> prefixes)
            throws InvalidDivideQueryParserInputException {
        // parse name of found graph
        if (graphName.startsWith("<")) {
            if (!graphName.endsWith(">")) {
                throw new InvalidDivideQueryParserInputException(
                        String.format("Invalid graph name '%s'", graphName));
            }
        } else {
            boolean matched = false;
            for (Prefix prefix : prefixes) {
                if (graphName.startsWith(prefix.getName())) {
                    matched = true;
                    String afterPrefix = graphName.replaceFirst(Pattern.quote(prefix.getName()), "");
                    graphName = String.format("%s%s>",
                            prefix.getUri().substring(0, prefix.getUri().length() - 1),
                            afterPrefix);
                    break;
                }
            }
            if (!matched) {
                throw new InvalidDivideQueryParserInputException(
                        String.format("Invalid graph name '%s' (no valid IRI" +
                                " & no existing prefix used)", graphName));
            }
        }
        return graphName;
    }

    /**
     * Processes the WHERE clause items in the parsed WHERE clause of a SPARQL
     * or RSP-QL query.
     * This processing is an additional parsing step: the WHERE clause items are split
     * based on whether they depend on the context, or depend on one or more streams
     * specified as stream graph IRIs in the input of the parser. The context expressions
     * are appended and returned as a string in the processed result, whereas the stream
     * expressions are still returned as an ordered list of items (i.e., either graph
     * patterns or expressions with a SPARQL keyword pattern outside a graph pattern).
     * While processing, the items are verified and an exception is thrown if anything
     * is not valid.
     *
     * @param whereClause parsed WHERE clause of SPARQL OR RSP-QL query
     * @param streamGraphNames names (IRIs) of stream graphs as specified in the parser input
     * @return the processed WHERE clause of the stream query
     * @throws InvalidDivideQueryParserInputException if an expression is present outside a graph without
     *                                                an associated SPARQL keyword (these should be
     *                                                placed inside a graph pattern), OR when an expression
     *                                                is present inside a graph that is not reading from
     *                                                a stream, but that contains a SPARQL pattern (because
     *                                                then it is part of the context, and this context cannot
     *                                                contain any SPARQL patterns, so this pattern should
     *                                                be placed outside the graph then)
     */
    private ParsedStreamQueryWhereClause parseStreamQueryWhereClauseOfQuery(
            WhereClause whereClause, List<String> streamGraphNames)
            throws InvalidDivideQueryParserInputException {
        // prepare results of parsing
        StringBuilder contextPart = new StringBuilder();
        List<WhereClauseItem> streamItems = new ArrayList<>();

        for (WhereClauseItem item : whereClause.getItems()) {
            if (item.getItemType() == WhereClauseItemType.EXPRESSION) {
                WhereClauseExpressionItem expressionItem = (WhereClauseExpressionItem) item;

                // expression items are verified and split based on SPARQL keywords,
                // and are included into the items that depend on the input stream(s)
                streamItems.addAll(
                        verifyAndSplitStreamQueryWhereClauseExpressionItemsBySparqlKeywords(expressionItem));

            } else if (item.getItemType() == WhereClauseItemType.GRAPH) {
                WhereClauseGraphItem graphItem = (WhereClauseGraphItem) item;

                // graph patterns are handled differently based on the specified name
                // of the graph in the pattern
                if (streamGraphNames.contains(graphItem.getGraph().getName())) {
                    // if the graph name is specified as a stream graph in the parser input,
                    // the whole pattern is included into the set of items that depend on
                    // the input stream(s)
                    streamItems.add(graphItem);
                } else {
                    // if the graph name is NOT specified as a stream graph, then it should
                    // first be checked that its expressions doe not contain any SPARQL
                    // keyword (because these will become the context part which will be
                    // added as the consequence of the sensor query rule, so it cannot contain
                    // any SPARQL keywords)
                    String graphItemLowerCaseClause =
                            graphItem.getGraph().getClause().toLowerCase(Locale.ROOT);
                    boolean containsSparqlKeyword = POSSIBLE_WHERE_CLAUSE_SPARQL_KEYWORDS
                            .stream()
                            .anyMatch(graphItemLowerCaseClause::contains);
                    if (containsSparqlKeyword) {
                        throw new InvalidDivideQueryParserInputException(
                                "Non-streaming graph patterns of stream query cannot contain " +
                                        "special SPARQL keywords - such expressions should " +
                                        "be placed outside the graph");
                    }

                    // if no SPARQL keyword is present, the expressions in the graph pattern
                    // can be safely added to the context part of the stream query WHERE clause
                    contextPart.append(graphItem.getGraph().getClause()).append(" ");
                }
            }
        }

        return new ParsedStreamQueryWhereClause(
                contextPart.toString().trim(), streamItems);
    }

    /**
     * Verifies and splits an individual expression item of the parsed WHERE clause of
     * the stream query. The splitting will split the individual items into a single part
     * per SPARQL keyword pattern. The verification is a check whether no expressions
     * occur in this expression item (i.e., outside a graph) without an associated
     * SPARQL keyword.
     *
     * @param expressionItem individual expression item of the parsed WHERE clause of
     *                       the stream query
     * @return a list of expression items originating from the original expression item,
     *         but split based on SPARQL keyword patterns
     * @throws InvalidDivideQueryParserInputException if an expression occurs in this expression
     *                                                item (i.e., outside a graph) without an
     *                                                associated SPARQL keyword (these should be
     *                                                placed inside a graph pattern)
     */
    private List<WhereClauseExpressionItem> verifyAndSplitStreamQueryWhereClauseExpressionItemsBySparqlKeywords(
            WhereClauseExpressionItem expressionItem) throws InvalidDivideQueryParserInputException {
        List<WhereClauseExpressionItem> resultItems = new ArrayList<>();
        String expressionLeftover = expressionItem.getExpression();

        // scan the expression for special SPARQL patterns, i.e., parts that start with
        // SPARQL keyword followed by any character but a keyword
        // (so if multiple keywords occur, there will be multiple matches)
        Matcher expressionMatcher = SPECIAL_SPARQL_PATTERN.matcher(
                expressionItem.getExpression());
        while (expressionMatcher.find()) {
            String match = expressionMatcher.group();

            // if the match involves a FILTER (NOT) EXISTS pattern, then the braces should
            // be scanned to find the end of the pattern
            // (instead of considering the end as the end of the pattern match)
            if (match.matches("^FILTER\\s+(NOT\\s+)?EXISTS\\s+\\{.*")) {
                // find end of FILTER (NOT) EXISTS pattern
                int braceCount = 1;
                int loopIndex = match.indexOf("{") + 1;
                while (braceCount > 0) {
                    char c = expressionLeftover.charAt(loopIndex);
                    if (c == '{') {
                        braceCount++;
                    } else if (c == '}') {
                        braceCount--;
                    }
                    loopIndex++;
                }

                // update match to reach from start to this end
                match = expressionLeftover.substring(0, loopIndex).trim();
            }

            // every match will be added as a separate WHERE clause expression item in the list
            resultItems.add(new WhereClauseExpressionItem(match));
            // this match is removed once from the original expression
            expressionLeftover = expressionLeftover.replaceFirst(
                    Pattern.quote(match), "").trim();
            // since the match can be made larger than the actual pattern match
            // (for FILTER (NOT) EXISTS patterns), a new matcher is created
            expressionMatcher = SPECIAL_SPARQL_PATTERN.matcher(expressionLeftover);
        }

        // if the original expression still contains text, this means that this part does
        // not start with a known SPARQL keyword => in that case, this expression should
        // be added to one of the graphs of the stream query WHERE clause
        if (!expressionLeftover.isEmpty()) {
            throw new InvalidDivideQueryParserInputException(
                    String.format("SPARQL pattern without known keyword found " +
                            "outside graph in stream query WHERE clause: %s", expressionLeftover));
        }
        return resultItems;
    }

    /**
     * Retrieves the input variables to be specified in a DIVIDE sensor query rule.
     * For this, it checks which variables occur in both the antecedent of the rule
     * (i.e., the context-dependent part of the stream query) and the RSP-QL query
     * body. These variables will be substituted into the RSP-QL query body after
     * the DIVIDE query derivation.
     *
     * @param contextPartOfSensorQueryRule context part of sensor query rule, i.e.,
     *                                     its antecedent
     * @param rspQlQueryBodyVariables unbound variables in RSP-QL query body that is
     *                                referenced in the sensor query rule via the
     *                                query pattern
     * @return the unbound variables that occur both in the antecedent and consequence
     *         of the sensor query rule, and that should therefore be specified as
     *         input variables
     */
    private List<String> retrieveInputVariables(String contextPartOfSensorQueryRule,
                                                Set<String> rspQlQueryBodyVariables) {
        List<String> antecedentVariables =
                findUnboundVariables(contextPartOfSensorQueryRule);

        return antecedentVariables
                .stream()
                .filter(rspQlQueryBodyVariables::contains)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves the output variables to be specified in a DIVIDE sensor query rule.
     * For this, it checks which variables occur in the output of the stream query
     * (this ends up in the consequence of the sensor query rule) that do NOT occur
     * in the context part of the sensor query rule; these variables are the output
     * variables that should be replaced by a blank node in the sensor query rule.
     *
     * @param contextPartOfSensorQueryRule context part of sensor query rule, i.e.,
     *                                     its antecedent
     * @param streamQueryResult output of the stream query
     * @return the unbound variables that occur in the output of the stream query,
     *         and not in the context part of the sensor query rule
     */
    private List<String> retrieveOutputVariables(String contextPartOfSensorQueryRule,
                                                 String streamQueryResult) {
        List<String> antecedentVariables =
                findUnboundVariables(contextPartOfSensorQueryRule);
        List<String> resultVariables = findUnboundVariables(streamQueryResult);

        return resultVariables
                .stream()
                .filter(s -> !antecedentVariables.contains(s))
                .collect(Collectors.toList());
    }

    /**
     * @param queryPart any part of a SPARQL or RSP-QL query (can also be the full query body)
     * @return a list of unbound variables that are present in the given query part
     */
    List<String> findUnboundVariables(String queryPart) {
        Matcher matcher = UNBOUND_VARIABLES_PATTERN.matcher(queryPart);
        Set<String> unboundVariables = new LinkedHashSet<>();
        while (matcher.find()) {
            String unboundVariable = matcher.group();
            unboundVariables.add(unboundVariable);
        }
        return new ArrayList<>(unboundVariables);
    }

    private List<String> findUnboundVariablesInWindowParameters(StreamWindow streamWindow) {
        Set<String> unboundVariables = new LinkedHashSet<>();
        String definition = streamWindow.getWindowDefinition();
        Matcher matcher = UNBOUND_VARIABLES_IN_STREAM_WINDOW_PATTERN.matcher(definition);
        while (matcher.find()) {
            unboundVariables.add("?" + matcher.group(1));
        }
        return new ArrayList<>(unboundVariables);
    }

    /**
     * Extends the output of the streaming query to be used in the consequence of
     * the sensor query rule. For this, it starts from the original stream query
     * output, and enriches it with all items in the stream-dependent parts of the
     * WHERE clause of the stream query.
     *
     * @param parsedStreamQueryWhereClauseStreamItems stream items occurring in parsed
     *                                                WHERE clause of stream query
     * @param streamQueryOutput defined output of stream query
     * @param prefixes prefixes to be used for the sensor query rule
     * @return extended output to be used in consequence of sensor query rule
     */
    private String extendOutputOfStreamQueryForSensorQueryRule(
            List<WhereClauseItem> parsedStreamQueryWhereClauseStreamItems,
            String streamQueryOutput,
            Set<Prefix> prefixes) throws InvalidDivideQueryParserInputException {
        Map<String, String> variableMapping = new HashMap<>();

        // create prefix string for queries
        String queryPrefixString = prefixes
                .stream()
                .map(prefix -> String.format("PREFIX %s %s",
                        prefix.getName(), prefix.getUri()))
                .collect(Collectors.joining(" "));

        // create prefix string in Turtle format
        String turtlePrefixString = divideQueryGenerator.getTurtlePrefixList(prefixes);

        // search all unbound variables in stream query output
        // -> sort to avoid any replacement issues later
        // -> create random unique mapping of variable to URI
        List<String> outputVariables = findUnboundVariables(streamQueryOutput);
        outputVariables = outputVariables
                .stream()
                .sorted((s1, s2) -> s1.contains(s2) ?
                        (s1.equals(s2) ? 0 : -1) :
                        (s2.contains(s1) ? 1 : s1.compareTo(s2)))
                .collect(Collectors.toList());
        for (String outputVariable : outputVariables) {
            variableMapping.put(outputVariable, createUnboundVariableMapping());
        }

        // create Jena model of sensor query rule output, combining both the
        // stream query output and the streaming part in its WHERE clause
        Model model = ModelFactory.createDefaultModel();

        // transform output to Turtle string with prefixes
        // -> map every unbound variable to its mapping
        // -> add statements to resulting Jena model
        String transformedOutput = turtlePrefixString + "\n" + streamQueryOutput;
        for (String outputVariable : outputVariables) {
            transformedOutput = transformedOutput.replace(
                    outputVariable, variableMapping.get(outputVariable));
        }
        Model transformedOutputModel = JenaUtilities.parseString(transformedOutput, RDFLanguage.TURTLE);
        if (transformedOutputModel == null) {
            throw new InvalidDivideQueryParserInputException(
                    "Parser will generate invalid output of sensor query rule. " +
                            "This is caused by an invalid stream query.");
        }
        model.add(transformedOutputModel);

        // loop over all streaming items in the WHERE clause of the stream query
        for (WhereClauseItem item : parsedStreamQueryWhereClauseStreamItems) {
            // retrieve content of graph
            String itemContent = "";
            if (item.getItemType() == WhereClauseItemType.EXPRESSION) {
                itemContent = ((WhereClauseExpressionItem) item).getExpression();
            } else if (item.getItemType() == WhereClauseItemType.GRAPH) {
                itemContent = ((WhereClauseGraphItem) item).getGraph().getClause();
            }

            // find unbound variables in this part & sort to avoid replacement issues
            List<String> itemUnboundVariables = findUnboundVariables(itemContent);
            itemUnboundVariables = itemUnboundVariables
                    .stream()
                    .sorted((s1, s2) -> s1.contains(s2) ?
                            (s1.equals(s2) ? 0 : -1) :
                            (s2.contains(s1) ? 1 : s1.compareTo(s2)))
                    .collect(Collectors.toList());

            // map all variables to existing or new mapping and replace
            // these variables by their mappings
            for (String itemUnboundVariable : itemUnboundVariables) {
                String mapping;
                if (variableMapping.containsKey(itemUnboundVariable)) {
                    mapping = variableMapping.get(itemUnboundVariable);
                } else {
                    mapping = createUnboundVariableMapping();
                    variableMapping.put(itemUnboundVariable, mapping);
                }
                itemContent = itemContent.replace(itemUnboundVariable, mapping);
            }

            // create Jena SPARQL query with this pattern
            // (required to filter out SPARQL patterns such as FILTER; if not filtered
            //  out, some WHERE clause patters will yield conversion issues since these
            //  SPARQL patterns are no valid Turtle
            String query = String.format("%s SELECT * WHERE { %s }", queryPrefixString, itemContent);
            Query q;
            try {
                q = QueryFactory.create(query);
            } catch (Exception e) {
                // temporarily disable exception since this only means that we will not include
                // this stream-dependent part of the WHERE clause in the sensor query rule's consequence
                // (is not really necessary either to perform the correct query derivation)
                // -> just ignore this where clause item and continue to next one
                // throw new InvalidDivideQueryParserInputException(
                //         String.format("Error in input which causes the following " +
                //                 "invalid parsed SPARQL clause: %s", itemContent));
                continue;
            }

            // only filter actual triple blocks in WHERE clause content
            final String[] processedItemContent = {""};
            ElementWalker.walk(q.getQueryPattern(),
                    new ElementVisitorBase() {
                        public void visit(ElementPathBlock el) {
                            processedItemContent[0] += el.toString();
                        }
                    }
            );

            // only proceed with this stream item if processed content is not empty
            // (if empty, this means it only consisted of SPARQL patterns)
            if (!processedItemContent[0].isEmpty()) {
                // replace variables created by Jena for blank nodes when parsing
                // the query WHERE clause -> these may be converted back to blank nodes,
                // since the result will be used to parse as Turtle
                processedItemContent[0] = processedItemContent[0].replace("??", "_:");

                // parse Turtle string of processed content as triples and
                // add them to resulting Jena model
                String turtleString = String.format("%s\n%s .",
                        turtlePrefixString,
                        processedItemContent[0]);
                Model parsed = JenaUtilities.parseString(
                        turtleString,
                        RDFLanguage.TURTLE);
                model.add(parsed);
            }
        }

        // create model with triples that involve any of the output variables
        /*Model result = ModelFactory.createDefaultModel();
        for (String outputVariable : outputVariables) {
            String mapping = variableMapping.get(outputVariable);
            mapping = mapping.substring(1, mapping.length() - 1);

            result.add(model.listStatements(new SimpleSelector(
                    new ResourceImpl(mapping), null, (Object) null)));
        }
        model = result;*/

        // clear prefixes to ensure output string does not contain any prefix definitions
        model.clearNsPrefixMap();

        // sort key set of mappings to avoid replacement issues
        List<String> sortedUnboundVariables = variableMapping.keySet()
                .stream()
                .sorted((s1, s2) -> s1.contains(s2) ?
                        (s1.equals(s2) ? 0 : -1) :
                        (s2.contains(s1) ? 1 : s1.compareTo(s2)))
                .collect(Collectors.toList());

        // create Turtle string of constructed resulting Jena model
        String extraOutput = JenaUtilities.serializeModel(model, RDFLanguage.TURTLE);

        // replace mappings of unbound variables back to the original unbound variable
        for (String unboundVariable : sortedUnboundVariables) {
            extraOutput = extraOutput.replace(
                    variableMapping.get(unboundVariable), unboundVariable);
        }

        return extraOutput;
    }

    private String createUnboundVariableMapping() {
        return String.format(
                "<http://idlab.ugent.be/divide/variable-mapping/%s>",
                UUID.randomUUID());
    }

    private String generateRandomUnboundVariable() {
        String base = "?" + UUID.randomUUID();
        return base.replace("-", "");
    }

    private Set<String> getUnboundVariablesInInput(DivideQueryParserInput input) {
        Set<String> unboundVariables = new HashSet<>(
                findUnboundVariables(input.getStreamQuery()));
        if (input.getFinalQuery() != null) {
            unboundVariables.addAll(findUnboundVariables(input.getFinalQuery()));
        }
        if (input.getIntermediateQueries() != null) {
            for (String intermediateQuery : input.getIntermediateQueries()) {
                unboundVariables.addAll(findUnboundVariables(intermediateQuery));
            }
        }
        return unboundVariables;
    }

    private CleanDivideQueryParserInput cleanInputFromOverlappingVariables(
            MappedDivideQueryParserInput input) {
        // create set of all existing unbound variables
        Set<String> unboundVariables = getUnboundVariablesInInput(input);

        // check if there is any unbound variable that is contained in another
        // -> label the longer variable as problematic, since we will change that one
        //    (in this way we avoid replacement issues in this method as well)
        List<String> problematicVariables = unboundVariables
                .stream()
                .filter(s -> unboundVariables.stream().anyMatch(
                        s1 -> s.contains(s1) && !s.equals(s1)))
                .collect(Collectors.toList());

        // if no such variable exists, then the same input can be used further on
        if (problematicVariables.isEmpty()) {
            CleanDivideQueryParserInput result =
                    new CleanDivideQueryParserInput(input);
            result.setUnboundVariables(unboundVariables);
            result.setFinalQueryVariableMapping(input.getFinalQueryVariableMapping());
            return result;
        }

        // otherwise, remove problematic variables from set of new variables
        Set<String> newVariables = unboundVariables
                .stream()
                .filter(s -> !problematicVariables.contains(s))
                .collect(Collectors.toSet());

        // sort problematic variables accordingly to avoid replacement issues
        List<String> sortedProblematicVariables = problematicVariables
                .stream()
                .sorted((s1, s2) -> s1.contains(s2) ?
                        (s1.equals(s2) ? 0 : -1) :
                        (s2.contains(s1) ? 1 : s1.compareTo(s2)))
                .collect(Collectors.toList());

        // create a mapping for each problematic variable to a new issue-less variable
        Map<String, String> variableMapping = new HashMap<>();
        for (String problematicVariable : sortedProblematicVariables) {
            String newVariable = null;
            boolean variableAccepted = false;
            while (!variableAccepted) {
                String triedNewVariable = generateRandomUnboundVariable();
                variableAccepted = newVariables.stream().noneMatch(triedNewVariable::contains);
                if (variableAccepted) {
                    newVariable = triedNewVariable;
                }
            }
            variableMapping.put(problematicVariable, newVariable);
        }

        // do in-order replacements of all new variables in each query
        String streamQuery = input.getStreamQuery();
        String finalQuery = input.getFinalQuery();
        List<String> intermediateQueries = input.getIntermediateQueries();
        String solutionModifier = input.getSolutionModifier();
        for (String problematicVariable : sortedProblematicVariables) {
            streamQuery = streamQuery.replaceAll(
                    Pattern.quote(problematicVariable),
                    variableMapping.get(problematicVariable));
            if (finalQuery != null) {
                finalQuery = finalQuery.replaceAll(
                        Pattern.quote(problematicVariable),
                        variableMapping.get(problematicVariable));
            }
            if (intermediateQueries != null) {
                intermediateQueries = intermediateQueries
                        .stream()
                        .map(s -> s.replaceAll(
                                Pattern.quote(problematicVariable),
                                variableMapping.get(problematicVariable)))
                        .collect(Collectors.toList());
            }
            if (solutionModifier != null) {
                solutionModifier = solutionModifier.replaceAll(
                        Pattern.quote(problematicVariable),
                        variableMapping.get(problematicVariable));
            }
        }

        CleanDivideQueryParserInput result =
                new CleanDivideQueryParserInput(
                        input.getInputQueryLanguage(),
                        input.getStreamWindows(),
                        streamQuery,
                        intermediateQueries,
                        finalQuery,
                        solutionModifier,
                        variableMapping);
        result.setUnboundVariables(getUnboundVariablesInInput(result));
        result.setFinalQueryVariableMapping(input.getFinalQueryVariableMapping());
        return result;
    }

    private ParsedStreamWindow parseStreamWindow(StreamWindow streamWindow,
                                                 Map<String, String> variableMapping)
            throws InvalidDivideQueryParserInputException {
        List<String> windowParameterVariables =
                findUnboundVariablesInWindowParameters(streamWindow);

        String streamIri = streamWindow.getStreamIri();
        String windowDefinition = streamWindow.getWindowDefinition();
        Map<String, String> defaultWindowParameterValues = streamWindow.getDefaultWindowParameterValues();

        Set<String> finalWindowParameterVariables = new HashSet<>();
        for (String windowParameterVariable : windowParameterVariables) {
            if (variableMapping.containsKey(windowParameterVariable)) {
                windowDefinition = windowDefinition.replaceAll(
                        Pattern.quote(String.format("?{%s}",
                                windowParameterVariable.substring(1))),
                        String.format("?{%s}", variableMapping.get(
                                windowParameterVariable).substring(1)));

                if (defaultWindowParameterValues.containsKey(windowParameterVariable)) {
                    String value = defaultWindowParameterValues.get(windowParameterVariable);
                    defaultWindowParameterValues.remove(windowParameterVariable);
                    defaultWindowParameterValues.put(variableMapping.get(windowParameterVariable), value);
                }

                finalWindowParameterVariables.add(
                        variableMapping.get(windowParameterVariable));

            } else {
                finalWindowParameterVariables.add(windowParameterVariable);
            }
        }

        for (String s : defaultWindowParameterValues.keySet()) {
            if (!finalWindowParameterVariables.contains(s)) {
                throw new InvalidDivideQueryParserInputException(String.format(
                        "Configuration of stream window with IRI '%s' contains default " +
                                "value for variable '%s' which does not occur in window definition",
                        streamIri, s));
            }
        }

        return new ParsedStreamWindow(
                streamIri, windowDefinition, defaultWindowParameterValues, finalWindowParameterVariables);
    }

    private List<String> parseSelectClause(String selectClause) {
        List<String> result = new ArrayList<>();
        String formattedSelectClause = String.format("%s ", selectClause.trim());
        if (SELECT_CLAUSE_PATTERN.matcher(formattedSelectClause).matches()) {
            Matcher m = SELECT_CLAUSE_PATTERN_ENTRY.matcher(formattedSelectClause);
            while (m.find()) {
                result.add(m.group().trim());
            }
        }
        return result;
    }

    private ConvertedStreamWindow convertParsedStreamWindow(ParsedStreamWindow parsedStreamWindow)
            throws InvalidDivideQueryParserInputException {
        String streamIri = parsedStreamWindow.getStreamIri();
        String windowDefinition = parsedStreamWindow.getWindowDefinition();
        Map<String, String> defaults = parsedStreamWindow.getDefaultWindowParameterValues();
        List<WindowParameter> windowParameters = new ArrayList<>();

        Matcher m = RSP_QL_WINDOW_PARAMETERS_PATTERN.matcher(windowDefinition);
        if (!m.find()) {
            throw new InvalidDivideQueryParserInputException("KLOPT NIET");
        }

        String range = m.group(2);
        String fromTo = m.group(4);
        String step = m.group(8);
        if (range != null) {
            String rangeParam = m.group(3);
            WindowParameter rangeWp = createWindowParameter(rangeParam, defaults);
            windowParameters.add(rangeWp);
            windowDefinition = windowDefinition.replaceAll(
                    Pattern.quote(rangeParam), String.format("?{%s}", rangeWp.getVariable().substring(1)));

        } else if (fromTo != null) {
            String fromParam = m.group(5);
            WindowParameter fromWp = createWindowParameter(fromParam, defaults);
            windowParameters.add(fromWp);
            windowDefinition = windowDefinition.replaceAll(
                    Pattern.quote(fromParam), String.format("?{%s}", fromWp.getVariable().substring(1)));

            String toParam = m.group(6);
            WindowParameter toWp = createWindowParameter(toParam, defaults);
            windowParameters.add(toWp);
            windowDefinition = windowDefinition.replaceAll(
                    Pattern.quote(toParam), String.format("?{%s}", toWp.getVariable().substring(1)));

        } else {
            throw new InvalidDivideQueryParserInputException("CANNOT MATCH??");
        }

        if (step != null) {
            String stepParam = m.group(9);
            WindowParameter stepWp = createWindowParameter(stepParam, defaults);
            windowParameters.add(stepWp);
            windowDefinition = windowDefinition.replaceAll(
                    Pattern.quote(stepParam), String.format("?{%s}", stepWp.getVariable().substring(1)));
        }

        return new ConvertedStreamWindow(
                streamIri,
                windowDefinition,
                windowParameters);
    }

    private WindowParameter createWindowParameter(String parameter,
                                                  Map<String, String> defaultWindowParameterValues)
            throws InvalidDivideQueryParserInputException {
        Matcher m = STREAM_WINDOW_PARAMETER_VARIABLE_PATTERN.matcher(parameter);
        if (m.find()) {
            String fullDuration = m.group(2);
            String number = m.group(13);

            String variableName;
            WindowParameter.WindowParameterType type = null;

            if (fullDuration != null) {
                variableName = extractNameFromWindowParameterVariable(fullDuration);
                type = WindowParameter.WindowParameterType.XSD_DURATION;

            } else if (number != null) {
                variableName = extractNameFromWindowParameterVariable(number);
                String typeString = m.group(23);
                if ("S".equals(typeString)) {
                    type = WindowParameter.WindowParameterType.TIME_SECONDS;
                } else if ("M".equals(typeString)) {
                    type = WindowParameter.WindowParameterType.TIME_MINUTES;
                } else if ("H".equals(typeString)) {
                    type = WindowParameter.WindowParameterType.TIME_HOURS;
                }

            } else {
                // impossible
                throw new InvalidDivideQueryParserInputException("IMPOSSIBLE");
            }

            // extract whether the variable is specified as a default or should be replaced
            // by the query derivation
            String defaultValue = defaultWindowParameterValues.getOrDefault("?" + variableName, null);
            if (defaultValue != null) {
                return new WindowParameter(
                        "?" + variableName,
                        defaultValue,
                        type,
                        false);
            } else {
                return new WindowParameter(
                        "?" + variableName,
                        "?" + variableName,
                        type,
                        true);
            }

        } else {
            Matcher m2 = STREAM_WINDOW_PARAMETER_NUMBER_PATTERN.matcher(parameter);
            if (m2.find()) {
                WindowParameter.WindowParameterType type = null;
                String value = m2.group(2);
                String typeString = m2.group(3);
                if ("S".equals(typeString)) {
                    type = WindowParameter.WindowParameterType.TIME_SECONDS;
                } else if ("M".equals(typeString)) {
                    type = WindowParameter.WindowParameterType.TIME_MINUTES;
                } else if ("H".equals(typeString)) {
                    type = WindowParameter.WindowParameterType.TIME_HOURS;
                }
                return new WindowParameter(
                        generateRandomUnboundVariable(),
                        value, type, false);
            } else {
                throw new InvalidDivideQueryParserInputException("invalid entry");
            }
        }
    }

    private String extractNameFromWindowParameterVariable(String variable) {
        return variable.substring(2, variable.length() - 1);
    }

    @SuppressWarnings("SameParameterValue")
    private void validateSparqlQuery(String query, String prefix)
            throws InvalidDivideQueryParserInputException {
        try {
            print("VALIDATING " + prefix.toUpperCase() + " QUERY: " + query);
            print("=======================================");
            QueryFactory.create(query);
        } catch (Exception e) {
            throw new InvalidDivideQueryParserInputException(
                    String.format("%s query is invalid SPARQL", prefix));
        }
    }

    private DivideQueryParserOutput restoreOriginalVariablesInOutput(DivideQueryParserOutput output,
                                                                     Map<String, String> variableMapping) {
        // do replacement for all variables
        String queryPattern = output.getQueryPattern();
        String sensorQueryRule = output.getSensorQueryRule();
        String goal = output.getGoal();
        for (String s : variableMapping.keySet()) {
            // do a first substitution specifically for the window definition
            queryPattern = queryPattern.replaceAll(
                    Pattern.quote(String.format("?{%s}", variableMapping.get(s).substring(1))),
                    String.format("?{%s}", s.substring(1)));

            queryPattern = queryPattern.replaceAll(
                    Pattern.quote(variableMapping.get(s)), s);
            sensorQueryRule = sensorQueryRule.replaceAll(
                    Pattern.quote(variableMapping.get(s)), s);
            goal = goal.replaceAll(
                    Pattern.quote(variableMapping.get(s)), s);
        }

        print("DOING OUTPUT MAPPING REPLACEMENT: " + variableMapping);
        print("======================================");

        return new DivideQueryParserOutput(
                queryPattern, sensorQueryRule, goal, output.getQueryForm());
    }

    @SuppressWarnings("unused")
    private DivideQueryParserOutput correctForStreamToFinalQueryVariableMapping(
            DivideQueryParserOutput result, Map<String, String> finalQueryVariableMapping) {
        // if no mapping exists of final query variables, then no further correction
        // to the output for this mapping should be done
        if (finalQueryVariableMapping == null || finalQueryVariableMapping.isEmpty()) {
            return result;
        }

        // a correction is only required for SELECT queries, since this output contains
        // bindings to actual variable names
        // -> these names should equal the original names that were used in the
        //    final query (for full transparency)
        // (note that we definitely know the input contained a final query, otherwise
        //  the final query variable mapping would be always empty)
        if (result.getQueryForm() == QueryForm.SELECT) {
            print("DOING ADDITIONAL CORRECTION IN SELECT QUERY FOR " +
                    "STREAM TO FINAL QUERY VARIABLE MAPPING");

            // extract query body from query pattern
            String queryPattern = result.getQueryPattern();
            String queryBody = extractRspQlQueryBodyFromQueryPattern(
                    queryPattern, result.getQueryForm());

            // only proceed if nothing went wrong with extracting the query body
            // -> otherwise return original result (better to have that result than
            //    no result of course)
            if ("".equals(queryBody) || queryBody == null) {
                return result;
            }

            // now replacements can be prepared
            // -> first generate set of all possible conflicting variables
            //    (conflicting for replacement later on)
            //    -> these are the keys and values of the mapping
            //    -> and also any other unbound variables occurring in the query body
            Set<String> conflictingVariables = new HashSet<>(finalQueryVariableMapping.keySet());
            conflictingVariables.addAll(finalQueryVariableMapping.values());
            List<String> rspQlQueryUnboundVariables = findUnboundVariables(queryBody);
            conflictingVariables.addAll(rspQlQueryUnboundVariables);

            // split replacement list in two to first do some temporal replacements
            // -> these replacements will be done first before doing the actual replacements
            // -> this is to avoid that conflicts occur with similar variables
            // -> this works if the resulting variables after replacement are unique, i.e.,
            //    they do not occur as such in the list of variables or as a substring of any
            //    of these variables (or vice versa)
            Map<String, String> temporalReplacements = new HashMap<>();
            Map<String, String> finalReplacements = new HashMap<>();
            for (Map.Entry<String, String> requiredReplacement : finalQueryVariableMapping.entrySet()) {
                // replacement is not required if:
                // - key and value are identical
                // - RSP-QL query body does not contain the key
                if (requiredReplacement.getKey().equals(requiredReplacement.getValue()) ||
                        !rspQlQueryUnboundVariables.contains(requiredReplacement.getKey())) {
                    continue;
                }

                // first check if replacement is allowed
                // -> this is not the case if the result of the replacement is already present
                //    as a variable in the RSP-QL query body
                if (rspQlQueryUnboundVariables.contains(requiredReplacement.getValue())) {
                    print(String.format("Cannot do replacement of %s to %s since the result " +
                            "is already present as a variable in the RSP-QL query body",
                            requiredReplacement.getKey(), requiredReplacement.getValue()));
                    continue;
                }

                String temporalVariable = "";
                boolean variableAccepted = false;
                while (!variableAccepted) {
                    String triedNewVariable = generateRandomUnboundVariable();
                    variableAccepted = conflictingVariables
                            .stream()
                            .noneMatch(s -> s.equals(triedNewVariable) ||
                                    s.contains(triedNewVariable) ||
                                    triedNewVariable.contains(s));
                    if (variableAccepted) {
                        temporalVariable = triedNewVariable;
                        conflictingVariables.add(triedNewVariable);
                    }
                }

                // split up replacements
                temporalReplacements.put(
                        requiredReplacement.getKey(), temporalVariable);
                finalReplacements.put(
                        temporalVariable, requiredReplacement.getValue());
            }

            print("Temporal replacements: " + temporalReplacements);
            print("Final replacements: " + finalReplacements);

            // first do temporal replacements
            List<String> sortedTemporalReplacementKeys = temporalReplacements.keySet()
                    .stream()
                    .sorted((s1, s2) -> s1.contains(s2) ?
                            (s1.equals(s2) ? 0 : -1) :
                            (s2.contains(s1) ? 1 : s1.compareTo(s2)))
                    .collect(Collectors.toList());
            String newQueryBody = queryBody;
            for (String key : sortedTemporalReplacementKeys) {
                newQueryBody = newQueryBody.replaceAll(
                        Pattern.quote(key), temporalReplacements.get(key));
            }

            // then also do final replacements
            List<String> finalTemporalReplacementKeys = finalReplacements.keySet()
                    .stream()
                    .sorted((s1, s2) -> s1.contains(s2) ?
                            (s1.equals(s2) ? 0 : -1) :
                            (s2.contains(s1) ? 1 : s1.compareTo(s2)))
                    .collect(Collectors.toList());
            for (String key : finalTemporalReplacementKeys) {
                newQueryBody = newQueryBody.replaceAll(
                        Pattern.quote(key), finalReplacements.get(key));
            }

            print("Old query body: " + queryBody);
            print("New query body: " + newQueryBody);
            print("======================================");

            queryPattern = queryPattern.replaceAll(
                    Pattern.quote(queryBody), newQueryBody);
            return new DivideQueryParserOutput(
                    queryPattern, result.getSensorQueryRule(),
                    result.getGoal(), result.getQueryForm());

        } else { // CONSTRUCT, DESCRIBE or ASK query
            // -> no adaptations to result part are required anymore, since the result
            //    contains no bindings to actual variable names, to these names do not
            //    need to be identical to the names in the original final query
            return result;
        }
    }

    private String extractRspQlQueryBodyFromQueryPattern(String queryPattern,
                                                         QueryForm queryForm) {
        final Pattern pattern = Pattern.compile(
                String.format(":%s \"\"\"(\\n|\\r|.)+\"\"\"\\.",
                        queryForm.toString().toLowerCase()));
        Matcher m = pattern.matcher(queryPattern);
        if (m.find()) {
            return m.group();
        } else {
            return "";
        }
    }
    
    private void print(String text) {
        if (DEBUG) {
            System.out.println(text);
        }
    }



    // TESTING METHODS

    public static void main(String[] args) throws Exception {
        DivideQueryParser parser = new DivideQueryParser();

        String query = IOUtilities.readFileIntoString("/home/mathias/Github/divide/divide-protego/protego-case/divide-queries/activity-brushing-teeth/sparql/stream-query.sparql");
        query = IOUtilities.removeWhiteSpace(query).replace("\r", "").trim();
        SplitSparqlQuery splitSparqlQuery = parser.splitSparqlQuery(query);
        System.out.println(splitSparqlQuery);

        Map<String, String> defaultWindowParameters = new HashMap<>();
        defaultWindowParameters.put("?seconds", "123M");
        String wd = "FROM NOW-?{seconds} TO NOW-PT?{otherSeconds}M STEP PT10S";
        StreamWindow sw = new StreamWindow("http://stream.test", wd, defaultWindowParameters);
        ParsedStreamWindow w = parser.parseStreamWindow(sw, Collections.emptyMap());
        ConvertedStreamWindow converted = parser.convertParsedStreamWindow(w);

        System.out.println(sw);
        System.out.println(w);
        System.out.println(converted);

        /*Matcher m = RSP_QL_WINDOW_PARAMETERS_PATTERN.matcher(wd);
        m.find();

        String range = m.group(2);
        String fromTo = m.group(4);
        String step = m.group(8);
        if (range != null) {
            String rangeParam = m.group(3);

        } else if (fromTo != null) {
            String fromParam = m.group(5);
            String toParam = m.group(6);

        } else {
            throw new InvalidDivideQueryParserInputException("CANNOT MATCH??");
        }

        if (step != null) {
            String stepParam = m.group(9);
            System.out.println(stepParam);
        }

        System.out.println("1:" + m.group(1));
        System.out.println("2:" + m.group(2));
        System.out.println("3:" + m.group(3));
        System.out.println("4:" + m.group(4));
        System.out.println("5:" + m.group(5));
        System.out.println("6:" + m.group(6));
        System.out.println("7:" + m.group(7));
        System.out.println("8:" + m.group(8));
        System.out.println("9:" + m.group(9));
        System.out.println("10:" + m.group(10));
        System.out.println("11:" + m.group(11));
        System.out.println("12:" + m.group(12));
        System.out.println("13:" + m.group(13));
        System.out.println("14:" + m.group(14));
        System.out.println("15:" + m.group(15));
        System.out.println("16:" + m.group(16));
        System.out.println("17:" + m.group(17));
        System.out.println("18:" + m.group(18));
        System.out.println("19:" + m.group(19));
        System.out.println("20:" + m.group(20));
        System.out.println("21:" + m.group(21));
        System.out.println("22:" + m.group(22));
        System.out.println("23:" + m.group(23));*/

        /*DivideQueryParserOutput divideQuery = singleSelectSparqlCase();

        System.out.println("Resulting DIVIDE query:");
        System.out.println("\nGOAL:\n" + divideQuery.getGoal());
        System.out.println("\nPATTERN:\n" + divideQuery.getQueryPattern());
        System.out.println("\nSENSOR QUERY RULE:\n" + divideQuery.getSensorQueryRule());*/
    }

    @SuppressWarnings("unused")
    private static void testStreamToFinalQueryVariableMapping()
            throws InvalidDivideQueryParserInputException{
        DivideQueryParser parser = new DivideQueryParser();

        List<StreamWindow> streamGraphs = new ArrayList<>();
        streamGraphs.add(new StreamWindow("<http://idlab.ugent.be/grove>",
                "RANGE PT?{seconds}S STEP PT3S",
                Collections.emptyMap()));
        List<String> intermediateQueries = new ArrayList<>();

        Map<String, String> mapping = new HashMap<>();
        mapping.put("?a", "?b");
        mapping.put("?b", "?c");

        String streamQuery =
                "?a test:K ?b .\n" +
                        "?c test:L ?d .";
        String finalQuery = "?a test:K ?b . ?c test:L ?d . ?ba test:M ?cd .";

        DivideQueryParserInput input = new DivideQueryParserInput(
                InputQueryLanguage.SPARQL,
                streamGraphs,
                streamQuery,
                intermediateQueries,
                finalQuery,
                "LIMIT 1",
                mapping);

        parser.parseDivideQuery(input);
    }

    @SuppressWarnings("unused")
    private static DivideQueryParserOutput singleSelectSparqlCase()
            throws InvalidDivideQueryParserInputException {
        DivideQueryParser parser = new DivideQueryParser();

        List<StreamWindow> streamGraphs = new ArrayList<>();
        streamGraphs.add(new StreamWindow("<http://idlab.ugent.be/grove>",
                "RANGE PT?{seconds}S STEP PT3S",
                Collections.emptyMap()));
        List<String> intermediateQueries = new ArrayList<>();
        Map<String, String> mapping = new HashMap<>();
        DivideQueryParserInput input = new DivideQueryParserInput(
                InputQueryLanguage.SPARQL,
                streamGraphs,
                IOUtilities.readFileIntoString(
                        "/home/mathias/Github/divide/divide-protego/dissect-case/divide-queries/" +
                                "3-no-activity-alarm-select/sparql-input/stream-query.query"),
                intermediateQueries,
                null,
                "LIMIT 1",
                mapping);

        return parser.parseDivideQuery(input);
    }

    @SuppressWarnings("unused")
    private static DivideQueryParserOutput singleSelectRspQlCase()
            throws InvalidDivideQueryParserInputException {
        DivideQueryParser parser = new DivideQueryParser();

        List<String> intermediateQueries = new ArrayList<>();
        DivideQueryParserInput input = new DivideQueryParserInput(
                InputQueryLanguage.RSP_QL,
                null,
                IOUtilities.readFileIntoString(
                        "/home/mathias/Github/divide/divide-protego/dissect-case/divide-queries/" +
                                "3-no-activity-alarm-select/rspql-input/stream-query.query"),
                intermediateQueries,
                null,
                "LIMIT 1",
                new HashMap<>());

        return parser.parseDivideQuery(input);
    }

    @SuppressWarnings("unused")
    private static DivideQueryParserOutput sparqlDescribeCase()
            throws InvalidDivideQueryParserInputException {
        DivideQueryParser parser = new DivideQueryParser();

        List<StreamWindow> streamGraphs = new ArrayList<>();
        streamGraphs.add(new StreamWindow("<http://idlab.ugent.be/grove>", "RANGE PT5S STEP PT3S",
                Collections.emptyMap()));
        List<String> intermediateQueries = new ArrayList<>();
        DivideQueryParserInput input = new DivideQueryParserInput(
                InputQueryLanguage.SPARQL,
                streamGraphs,
                IOUtilities.readFileIntoString(
                        "/home/mathias/Github/divide/divide-protego/dissect-case/divide-queries/" +
                                "1-above-threshold-alarm-describe/sparql-input/stream-query.query"),
                intermediateQueries,
                IOUtilities.readFileIntoString(
                        "/home/mathias/Github/divide/divide-protego/dissect-case/divide-queries/" +
                                "1-above-threshold-alarm-describe/sparql-input/final-query.query"),
                "ORDER BY DESC(?t) LIMIT 1",
                new HashMap<>());

        return parser.parseDivideQuery(input);
    }

    @SuppressWarnings("unused")
    private static DivideQueryParserOutput sparqlAskCase()
            throws InvalidDivideQueryParserInputException {
        DivideQueryParser parser = new DivideQueryParser();

        List<StreamWindow> streamGraphs = new ArrayList<>();
        streamGraphs.add(new StreamWindow("<http://idlab.ugent.be/grove>", "RANGE PT5S STEP PT3S",
                Collections.emptyMap()));
        List<String> intermediateQueries = new ArrayList<>();
        DivideQueryParserInput input = new DivideQueryParserInput(
                InputQueryLanguage.SPARQL,
                streamGraphs,
                IOUtilities.readFileIntoString(
                        "/home/mathias/Github/divide/divide-protego/dissect-case/divide-queries/" +
                                "1-above-threshold-alarm-ask/sparql-input/stream-query.query"),
                intermediateQueries,
                IOUtilities.readFileIntoString(
                        "/home/mathias/Github/divide/divide-protego/dissect-case/divide-queries/" +
                                "1-above-threshold-alarm-ask/sparql-input/final-query.query"),
                "ORDER BY DESC(?t) LIMIT 1",
                new HashMap<>());

        return parser.parseDivideQuery(input);
    }

    @SuppressWarnings("unused")
    private static DivideQueryParserOutput singleSparqlQueryCase()
            throws InvalidDivideQueryParserInputException {
        DivideQueryParser parser = new DivideQueryParser();

        List<StreamWindow> streamGraphs = new ArrayList<>();
        streamGraphs.add(new StreamWindow("<http://idlab.ugent.be/grove>",
                "FROM NOW-PT35M TO NOW-PT5M STEP PT5S",
                Collections.emptyMap()));
        streamGraphs.add(new StreamWindow("<http://idlab.ugent.be/grove2>",
                "RANGE PT5S TUMBLING",
                Collections.emptyMap()));
        List<String> intermediateQueries = new ArrayList<>();
        DivideQueryParserInput input = new DivideQueryParserInput(
                InputQueryLanguage.SPARQL,
                streamGraphs,
                IOUtilities.readFileIntoString(
                        "/home/mathias/Github/divide/divide-protego/query_matching/" +
                                "case_noAdditionalReasoning/case_1_concussion/input-query-converted.query"),
                intermediateQueries,
                null,
                null,
                new HashMap<>());

        return parser.parseDivideQuery(input);
    }

    @SuppressWarnings("unused")
    private static DivideQueryParserOutput doubleSparqlQueryCase()
            throws InvalidDivideQueryParserInputException {
        DivideQueryParser parser = new DivideQueryParser();

        List<StreamWindow> streamGraphs = new ArrayList<>();
        streamGraphs.add(new StreamWindow("<http://idlab.ugent.be/grove>",
                "RANGE PT5S STEP PT1S",
                Collections.emptyMap()));
        List<String> intermediateQueries = new ArrayList<>();
        DivideQueryParserInput input = new DivideQueryParserInput(
                InputQueryLanguage.SPARQL,
                streamGraphs,
                IOUtilities.readFileIntoString(
                        "/home/mathias/Github/divide/divide-protego/query_matching/" +
                                "case_additionalReasoning/case_1_concussion/input-query-converted.query"),
                intermediateQueries,
                IOUtilities.readFileIntoString(
                        "/home/mathias/Github/divide/divide-protego/query_matching/" +
                                "case_additionalReasoning/case_1_concussion/streamfox/reasoning-query-2.query"),
                null,
                new HashMap<>());

        return parser.parseDivideQuery(input);
    }

    @SuppressWarnings("unused")
    private static DivideQueryParserOutput tripleSparqlQueryCase()
            throws InvalidDivideQueryParserInputException {
        DivideQueryParser parser = new DivideQueryParser();

        List<StreamWindow> streamGraphs = new ArrayList<>();
        streamGraphs.add(new StreamWindow("<http://idlab.ugent.be/grove>",
                "RANGE PT5S STEP PT1S",
                Collections.emptyMap()));
        List<String> intermediateQueries = new ArrayList<>();
        intermediateQueries.add(IOUtilities.readFileIntoString(
                "/home/mathias/Github/divide/divide-protego/query_matching/" +
                        "case_additionalReasoning/case_2_concussion/streamfox/reasoning-query-2.query"));
        DivideQueryParserInput input = new DivideQueryParserInput(
                InputQueryLanguage.SPARQL,
                streamGraphs,
                IOUtilities.readFileIntoString(
                        "/home/mathias/Github/divide/divide-protego/query_matching/" +
                                "case_additionalReasoning/case_2_concussion/input-query-converted.query"),
                intermediateQueries,
                IOUtilities.readFileIntoString(
                        "/home/mathias/Github/divide/divide-protego/query_matching/" +
                                "case_additionalReasoning/case_2_concussion/streamfox/reasoning-query-3.query"),
                "GROUP BY ?v LIMIT 2",
                new HashMap<>());

        return parser.parseDivideQuery(input);
    }

    @SuppressWarnings("unused")
    private static DivideQueryParserOutput singleRspQlQueryCase()
            throws InvalidDivideQueryParserInputException {
        DivideQueryParser parser = new DivideQueryParser();

        List<String> intermediateQueries = new ArrayList<>();
        DivideQueryParserInput input = new DivideQueryParserInput(
                InputQueryLanguage.RSP_QL,
                null,
                IOUtilities.readFileIntoString(
                        "/home/mathias/Github/divide/divide-protego/query_matching/" +
                                "case_noAdditionalReasoning/case_1_concussion/input-query-converted_RSPQL.query"),
                intermediateQueries,
                null,
                null,
                new HashMap<>());

        return parser.parseDivideQuery(input);
    }

    @Override
    public String getTurtlePrefixList(Set<Prefix> prefixes) {
        return divideQueryGenerator.getTurtlePrefixList(prefixes);
    }

}
