package be.ugent.idlab.divide.core.query.parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

class DivideQueryGenerator {

    static long COUNTER = 1;

    private static final Set<Prefix> DIVIDE_PREFIXES = new HashSet<>();
    private static final Set<String> DIVIDE_PREFIX_NAMES;
    private static final Map<String, Prefix> DIVIDE_PREFIX_MAP;

    static {
        DIVIDE_PREFIXES.add(new Prefix(":", "<http://idlab.ugent.be/sensdesc/query#>"));
        DIVIDE_PREFIXES.add(new Prefix("sd:", "<http://idlab.ugent.be/sensdesc#>"));
        DIVIDE_PREFIXES.add(new Prefix("sh:", "<http://www.w3.org/ns/shacl#>"));
        DIVIDE_PREFIXES.add(new Prefix("owl:", "<http://www.w3.org/2002/07/owl#>"));
        DIVIDE_PREFIXES.add(new Prefix("rdf:", "<http://www.w3.org/1999/02/22-rdf-syntax-ns#>"));
        DIVIDE_PREFIXES.add(new Prefix("xsd:", "<http://www.w3.org/2001/XMLSchema#>"));

        DIVIDE_PREFIX_MAP = new HashMap<>();
        for (Prefix dividePrefix : DIVIDE_PREFIXES) {
            DIVIDE_PREFIX_MAP.put(dividePrefix.getName(), dividePrefix);
        }

        DIVIDE_PREFIX_NAMES = DIVIDE_PREFIX_MAP.keySet();
    }

    private static final String TURTLE_PREFIX_TEMPLATE = "@prefix %s %s .";

    private static final String SHACL_PREFIX_DECLARATION_TEMPLATE =
            ":prefixes-%d sh:declare [ sh:prefix \"%s\" ; sh:namespace \"%s\"^^xsd:anyURI ] .";

    private static final String QUERY_PATTERN_TEMPLATE =
            "%s\n" + ":prefixes-%d rdf:type owl:Ontology .\n%s\n" +
                    ":pattern rdf:type sd:QueryPattern ; " +
                    "sh:prefixes :prefixes-%d ; sh:%s \"\"\"%s\"\"\".";

    private static final String SENSOR_QUERY_RULE_TEMPLATE =
            "%s\n" +
                    "{\n%s\n}\n=>\n{\n" +
                    "_:q rdf:type sd:Query ;\n" +
                    "    sd:pattern :pattern ;\n" +
                    "    sd:inputVariables (%s) ;\n" +
                    "    sd:windowParameters (%s) ;\n" +
                    "    sd:outputVariables (%s) .\n" +
                    "\n%s\n} .";

    private static final String SENSOR_QUERY_RULE_ADDITIONAL_RULE_TEMPLATE =
            "{\n%s\n}\n=>\n{\n%s\n} .";

    private static final String SENSOR_QUERY_RULE_INPUT_OUTPUT_VARIABLE_TEMPLATE = "(\"%s\" %s)";

    private static final String SENSOR_QUERY_RULE_WINDOW_PARAMETER_TEMPLATE = "(\"%s\" %s %s)";

    private static final Map<WindowParameter.WindowParameterType, String>
            windowParameterTypeMapping = new HashMap<>();
    static {
        windowParameterTypeMapping.put(WindowParameter.WindowParameterType.XSD_DURATION,
                "<http://www.w3.org/2001/XMLSchema#duration>");
        windowParameterTypeMapping.put(WindowParameter.WindowParameterType.TIME_SECONDS,
                "<http://www.w3.org/2006/time#seconds>");
        windowParameterTypeMapping.put(WindowParameter.WindowParameterType.TIME_MINUTES,
                "<http://www.w3.org/2006/time#minutes>");
        windowParameterTypeMapping.put(WindowParameter.WindowParameterType.TIME_HOURS,
                "<http://www.w3.org/2006/time#hours>");
    }

    private static final String GOAL_TEMPLATE = "%s\n{\n%s\n}\n=>\n{\n%s\n} .";

    private static final String RSP_QL_QUERY_BODY_TEMPLATE = "%s\n%s\n%s\nWHERE {\n%s\n}\n%s";

    private static final String RSP_QL_QUERY_BODY_FROM_TEMPLATE =
            "FROM NAMED WINDOW :win%d ON %s [%s]";

    private static final String RSP_QL_QUERY_BODY_WHERE_GRAPH_TEMPLATE = "WINDOW :win%d {\n%s\n}";

    /**
     * @param queryForm query form of the RSP-QL query template for which
     *                  this pattern is created
     * @param prefixes set of prefixes used in the RSP-QL query body
     * @param rspQlQueryBody RSP-QL query body of the query template for which
     *                       this pattern is created; this should be the output
     *                       of the {@link #createRspQlQueryBody(QueryForm, String,
     *                       List, String, List, DivideQueryParser)} method
     *
     * @return query pattern of the DIVIDE query
     */
    String createQueryPattern(QueryForm queryForm,
                              Set<Prefix> prefixes,
                              String rspQlQueryBody) {
        Set<Prefix> dividePrefixes = new HashSet<>(DIVIDE_PREFIXES);

        // loop over prefixes
        Set<Prefix> prefixesPresent = new HashSet<>();
        for (Prefix prefix : prefixes) {
            if (Pattern.compile("(\\s|\\(|^|\\^)" + prefix.getName() + "(?!win\\d+\\s)")
                    .matcher(rspQlQueryBody).find()) {
                if (":".equals(prefix.getName())) {
                    // a prefix without a name cannot be defined in SHACL, so should
                    // be replaced with a DIVIDE prefix
                    Prefix newPrefix = new Prefix(
                            String.format("divide-%s:", UUID.randomUUID()),
                            prefix.getUri());

                    // update prefix set
                    prefixesPresent.add(newPrefix);

                    // update RSP-QL query body according to new prefix
                    Pattern replacingPattern = Pattern.compile("(\\s|\\(|^|\\^):(?!win\\d+\\s)");
                    Matcher m = replacingPattern.matcher(rspQlQueryBody);
                    rspQlQueryBody = m.replaceAll("$1" + newPrefix.getName());

                } else {
                    // only include in prefix set if prefix occurs in RSP-QL query body
                    prefixesPresent.add(prefix);
                }
            }
        }

        // update DIVIDE prefixes and template if prefix conflicts exist
        List<String> templates = new ArrayList<>();
        templates.add(QUERY_PATTERN_TEMPLATE);
        templates.add(SHACL_PREFIX_DECLARATION_TEMPLATE);
        templates = solveConflictsWithDividePrefixes(templates, prefixesPresent, dividePrefixes);

        return String.format(templates.get(0),
                getTurtlePrefixList(dividePrefixes),
                COUNTER,
                getShaclPrefixList(prefixesPresent, templates.get(1)),
                COUNTER,
                queryForm.toString().toLowerCase(),
                rspQlQueryBody);
    }

    /**
     * @param prefixes set of prefixes used in the sensor query rule content
     *                 (both context part and stream query result, and in possible
     *                 additional queries)
     * @param contextPart context part of stream query which is used as antecedent
     *                    of the sensor query rule
     * @param streamQueryResult stream query result which is part of the consequence
     *                          of the sensor query rule
     * @param inputVariables input variables from the antecedent that need to be substituted
     *                       into the consequence (including pattern) and therefore need to
     *                       be defined as input variables in this sensor query rule
     * @param outputVariables output variables in the stream query result (i.e., variables
     *                        not occurring in the antecedent) that therefore need to be
     *                        substituted into blank nodes in the consequence of the created
     *                        sensor query rule
     * @param additionalQueries parsed additional SPARQL queries that are executed
     *                          between the first stream-dependent query and the final
     *                          query yielding the query result
     *
     * @return sensor query rule for the DIVIDE query, extended with an addition rule
     *         for each additional query (if existing)
     */
    String createSensorQueryRule(Set<Prefix> prefixes,
                                 String contextPart,
                                 String streamQueryResult,
                                 List<String> inputVariables,
                                 List<WindowParameter> windowParameters,
                                 List<String> outputVariables,
                                 List<ParsedSparqlQuery> additionalQueries) {
        Set<Prefix> dividePrefixes = new HashSet<>(DIVIDE_PREFIXES);

        // update DIVIDE prefixes and template if prefix conflicts exist
        List<String> templates = Collections.singletonList(SENSOR_QUERY_RULE_TEMPLATE);
        templates = solveConflictsWithDividePrefixes(templates, prefixes, dividePrefixes);

        // merge all prefixes
        // (merging can happen without any issues since the documentation
        //  mentions that this method expects no overlap between the prefix
        //  sets)
        Set<Prefix> allPrefixes = new HashSet<>(dividePrefixes);
        allPrefixes.addAll(prefixes);

        // generate string of input variables
        String inputVariablesString = inputVariables
                .stream()
                .sorted((s1, s2) -> s1.contains(s2) ?
                        (s1.equals(s2) ? 0 : -1) :
                        (s2.contains(s1) ? 1 : s1.compareTo(s2)))
                .map(s -> String.format(SENSOR_QUERY_RULE_INPUT_OUTPUT_VARIABLE_TEMPLATE, s, s))
                .collect(Collectors.joining(" "));

        // generate string of window variables
        String windowVariablesString = windowParameters
                .stream()
                .map(s -> String.format(SENSOR_QUERY_RULE_WINDOW_PARAMETER_TEMPLATE,
                        s.getVariable(),
                        !s.isValueSubstitutionVariable() &&
                                s.getType() == WindowParameter.WindowParameterType.XSD_DURATION ?
                                "\"" + s.getValue() + "\"" : s.getValue(),
                        windowParameterTypeMapping.get(s.getType())))
                .collect(Collectors.joining(" "));

        // process all output variables
        List<String> outputVariablesList = new ArrayList<>();
        for (String outputVariable : outputVariables) {
            // create blank node for each output variable
            String blank = outputVariable.replaceFirst(Pattern.quote("?"), "_:");

            // generate string to add to list output variables
            outputVariablesList.add(String.format(
                    SENSOR_QUERY_RULE_INPUT_OUTPUT_VARIABLE_TEMPLATE,
                    outputVariable, blank));

            // replace output variable by its blank node in the stream query result,
            // which ends up in the consequence of the sensor query rule
            streamQueryResult = streamQueryResult.replaceAll(
                    Pattern.quote(outputVariable), blank);
        }
        String outputVariablesString = String.join(" ", outputVariablesList);

        // create sensor query rule string
        String sensorQueryRule = String.format(templates.get(0),
                getTurtlePrefixList(allPrefixes),
                contextPart,
                inputVariablesString,
                windowVariablesString,
                outputVariablesString,
                streamQueryResult);

        // create additional rule string for each additional query
        // (WHERE clause as antecedent, CONSTRUCT clause as consequence)
        List<String> additionalRules = new ArrayList<>();
        for (ParsedSparqlQuery additionalQuery : additionalQueries) {
            additionalRules.add(String.format(SENSOR_QUERY_RULE_ADDITIONAL_RULE_TEMPLATE,
                    additionalQuery.getSplitSparqlQuery().getWherePart(),
                    additionalQuery.getSplitSparqlQuery().getResultPart()));
        }

        // create the actual sensor query rule and append the additional rules to it
        return String.format("%s\n\n%s",
                sensorQueryRule,
                String.join("\n\n", additionalRules));
    }

    /**
     * @param prefixes set of prefixes used in the goal's antecedent and consequence
     * @param antecedent antecedent of the rule that makes up the goal
     * @param consequence consequence of the rule that makes up the goal
     *
     * @return goal for the DIVIDE query
     */
    String createGoal(Set<Prefix> prefixes,
                      String antecedent,
                      String consequence) {
        String prefixString = String.join(" ", getTurtlePrefixList(prefixes));
        return String.format(GOAL_TEMPLATE,
                prefixString,
                antecedent,
                consequence);
    }

    /**
     * @param queryForm form of the last query in the chain of input queries, that also
     *                  needs to be used as form in the RSP-QL query body for the DIVIDE
     *                  query (can either be CONSTRUCT, SELECT or ASK)
     * @param queryOutput output of the last query in the chain of input queries, that
     *                    also needs to be the output of the RSP-QL query body for the
     *                    DIVIDE query
     * @param whereClauseItems ordered list of WHERE clause items that are either graphs
     *                         clauses on a stream IRI or SPARQL expressions; this list
     *                         will be processed to generate the WHERE clause for the
     *                         created RSP-QL query body; this list should contain at
     *                         least 1 graph clause on a stream IRI
     * @param solutionModifier solution modifier of the resulting RSP-QL query as defined
     *                         in the input
     * @param streamWindows stream windows defined in the parser input, which should contain
     *                      an entry for each stream IRI specified in the graph WHERE clause
     *                      items (together with the window parameters for this stream IRI)
     *
     * @return the RSP-QL query body to be used in the query pattern of the DIVIDE query
     *
     * @throws InvalidDivideQueryParserInputException if the stream windows list does not contain
     *                                                a stream window with a graph IRI that appears
     *                                                in the where clause graph items that make up
     *                                                the RSP-QL query body
     */
    RspQlQueryBody createRspQlQueryBody(QueryForm queryForm,
                                        String queryOutput,
                                        List<WhereClauseItem> whereClauseItems,
                                        String solutionModifier,
                                        List<ConvertedStreamWindow> streamWindows,
                                        DivideQueryParser parser)
            throws InvalidDivideQueryParserInputException {
        // create set of distinct stream graph names (IRIs) in the set of
        // WHERE clause items
        Set<String> inputStreamGraphs = new HashSet<>();
        for (WhereClauseItem whereClauseItem : whereClauseItems) {
            if (whereClauseItem.getItemType() == WhereClauseItemType.GRAPH) {
                WhereClauseGraphItem graphItem =
                        (WhereClauseGraphItem) whereClauseItem;
                inputStreamGraphs.add(graphItem.getGraph().getName());
            }
        }

        // keep track of to which window number the different stream graph
        // names are mapped
        Map<String, Integer> streamGraphToWindowNumberMap = new HashMap<>();

        // create FROM clauses
        List<String> fromParts = new ArrayList<>();
        int windowCounter = 0;
        for (String inputStreamGraph : inputStreamGraphs) {
            // filter list of input stream windows with the window that
            // has the same IRI (name)
            Optional<ConvertedStreamWindow> matchingWindow = streamWindows
                    .stream()
                    .filter(streamWindow -> streamWindow.getStreamIri().equals(inputStreamGraph))
                    .findFirst();

            // if such a window is not present, an exception should be thrown,
            // because then there is no input about the window parameters for
            // this IRI
            if (!matchingWindow.isPresent()) {
                throw new InvalidDivideQueryParserInputException(
                        String.format("Window parameters of input stream '%s' are not " +
                                "specified in input", inputStreamGraph));
            }

            // otherwise, the FROM clause of this window can be generated and the
            // window number is saved to the map
            // -> first, the unbound variables still need to be replaced in window
            String windowDefinition = matchingWindow.get().getWindowDefinition();
            fromParts.add(String.format(RSP_QL_QUERY_BODY_FROM_TEMPLATE,
                    windowCounter,
                    inputStreamGraph,
                    windowDefinition));
            streamGraphToWindowNumberMap.put(inputStreamGraph, windowCounter++);
        }
        String fromPart = String.join("\n", fromParts);

        // construct WHERE clause
        StringBuilder whereClause = new StringBuilder();
        if (inputStreamGraphs.size() == 1) {
            // if there is only 1 input stream graph, all expressions in the WHERE
            // clause items can be grouped under that same graph
            String graphName = "";
            for (WhereClauseItem whereClauseItem : whereClauseItems) {
                if (whereClauseItem.getItemType() == WhereClauseItemType.EXPRESSION) {
                    WhereClauseExpressionItem expressionItem =
                            (WhereClauseExpressionItem) whereClauseItem;
                    whereClause.append(expressionItem.getExpression()).append(" ");

                } else if (whereClauseItem.getItemType() == WhereClauseItemType.GRAPH) {
                    WhereClauseGraphItem graphItem =
                            (WhereClauseGraphItem) whereClauseItem;
                    whereClause.append(graphItem.getGraph().getClause()).append(" ");
                    graphName = graphItem.getGraph().getName();

                }
            }

            // this means 1 graph pattern is created on the window with the correct number
            // -> this makes up the whole WHERE clause of the query
            whereClause = new StringBuilder(String.format(RSP_QL_QUERY_BODY_WHERE_GRAPH_TEMPLATE,
                    streamGraphToWindowNumberMap.get(graphName),
                    whereClause));

        } else {
            // if there is more than 1 input stream graph, all expressions in the WHERE
            // clause items (that were not grouped under a graph, i.e., of item type EXPRESSION)
            // are also appended to the WHERE clause in the same way (i.e., not under a graph)
            List<String> whereClauseParts = new ArrayList<>();
            for (WhereClauseItem whereClauseItem : whereClauseItems) {
                if (whereClauseItem.getItemType() == WhereClauseItemType.EXPRESSION) {
                    WhereClauseExpressionItem expressionItem =
                            (WhereClauseExpressionItem) whereClauseItem;
                    // so expression items are just added as such to the WHERE clause
                    whereClauseParts.add(expressionItem.getExpression());

                } else if (whereClauseItem.getItemType() == WhereClauseItemType.GRAPH) {
                    WhereClauseGraphItem graphItem =
                            (WhereClauseGraphItem) whereClauseItem;
                    // graph items are added as a graph pattern on the window with the correct number
                    whereClauseParts.add(String.format(RSP_QL_QUERY_BODY_WHERE_GRAPH_TEMPLATE,
                            streamGraphToWindowNumberMap.get(graphItem.getGraph().getName()),
                            graphItem.getGraph().getClause()));

                }
            }
            // in this case, the WHERE clause consists of the ordered string of all
            // created individual parts
            whereClause = new StringBuilder(String.join("\n", whereClauseParts));
        }

        // generate query body string
        String queryFormString = queryForm == QueryForm.CONSTRUCT
                ? String.format("{ %s }", queryOutput)
                : (queryForm == QueryForm.ASK ? "" : queryOutput);
        String queryBody = String.format(RSP_QL_QUERY_BODY_TEMPLATE,
                queryForm.toString(),
                queryFormString,
                fromPart,
                whereClause,
                solutionModifier);

        // collect all unbound variables in RSP-QL query body
        // -> ignore from part when doing general search
        // -> ignore unbound variables in stream windows since they will be used
        //    as window parameters instead of input variables
        Set<String> unboundVariables = new HashSet<>(
                parser.findUnboundVariables(String.format(RSP_QL_QUERY_BODY_TEMPLATE,
                        queryForm,
                        queryFormString,
                        "",
                        whereClause,
                        solutionModifier)));

        return new RspQlQueryBody(queryBody, unboundVariables, queryForm,
                queryFormString, whereClause.toString());
    }

    /**
     * Solves any conflicts with the set of used prefixes in the given template
     * and with the given set of DIVIDE prefixes.
     *
     * @param templates templates to be checked
     * @param usedPrefixes set of prefixes that is used, with which no conflicts
     *                     may occur
     * @param dividePrefixes set of prefixes that will be used for the DIVIDE IRIs
     *                       in the given template; this set will be modified if
     *                       any prefix conflicts occur (conflicting prefixes are
     *                       then replaced by the new unambiguous ones)
     * @return modified templates where any prefix conflicts are resolved, i.e.,
     *         where conflicting prefixes are replaced by an unambiguous new one
     */
    private List<String> solveConflictsWithDividePrefixes(List<String> templates,
                                                          Set<Prefix> usedPrefixes,
                                                          Set<Prefix> dividePrefixes) {
        for (Prefix prefix : usedPrefixes) {
            if (DIVIDE_PREFIX_NAMES.contains(prefix.getName())) {
                // retrieve prefix
                Prefix conflictingPrefix = DIVIDE_PREFIX_MAP.get(prefix.getName());

                // it is only a real conflict if the URI differs
                if (!prefix.getUri().equals(conflictingPrefix.getUri())) {
                    // create new prefix
                    Prefix newPrefix = new Prefix(
                            String.format("divide-%s:", UUID.randomUUID()),
                            conflictingPrefix.getUri());

                    // update prefix set
                    dividePrefixes.remove(conflictingPrefix);
                    dividePrefixes.add(newPrefix);

                    // update prefix template
                    List<String> newTemplates = new ArrayList<>();
                    for (String template : templates) {
                        Pattern replacingPattern =
                                Pattern.compile("(\\s|\\(|^|\\^)" + conflictingPrefix.getName());
                        Matcher m = replacingPattern.matcher(template);
                        template = m.replaceAll("$1" + newPrefix.getName());
                        newTemplates.add(template);
                    }
                    templates = new ArrayList<>(newTemplates);
                }
            }
        }
        return templates;
    }

    String getTurtlePrefixList(Set<Prefix> prefixes) {
        List<String> turtlePrefixList = new ArrayList<>();
        for (Prefix prefix : prefixes) {
            turtlePrefixList.add(convertPrefixToTurtlePrefix(prefix));
        }
        return String.join(" ", turtlePrefixList);
    }

    private String getShaclPrefixList(Set<Prefix> prefixes, String template) {
        List<String> shaclPrefixList = new ArrayList<>();
        for (Prefix prefix : prefixes) {
            shaclPrefixList.add(convertPrefixToShaclPrefix(template, prefix));
        }
        return String.join("\n", shaclPrefixList);
    }

    private String convertPrefixToTurtlePrefix(Prefix prefix) {
        return String.format(TURTLE_PREFIX_TEMPLATE, prefix.getName(), prefix.getUri());
    }

    private String convertPrefixToShaclPrefix(String template, Prefix prefix) {
        return String.format(template,
                COUNTER,
                prefix.getName().substring(0, prefix.getName().length() - 1),
                prefix.getUri().substring(1, prefix.getUri().length() - 1));
    }

}
