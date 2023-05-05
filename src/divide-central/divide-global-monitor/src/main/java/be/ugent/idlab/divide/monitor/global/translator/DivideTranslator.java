package be.ugent.idlab.divide.monitor.global.translator;

import be.ugent.idlab.divide.core.engine.DivideEngineFactory;
import be.ugent.idlab.divide.core.engine.IDivideEngine;
import be.ugent.idlab.divide.core.exception.DivideNotInitializedException;
import be.ugent.idlab.divide.rsp.RspLocation;
import be.ugent.idlab.divide.util.Constants;
import be.ugent.idlab.util.io.IOUtilities;
import be.ugent.idlab.util.rdf.RDFLanguage;
import be.ugent.idlab.util.rdf.jena3.owlapi4.JenaUtilities;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.impl.PropertyImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class DivideTranslator {

    private static final Logger LOGGER = LoggerFactory.getLogger(DivideTranslator.class.getName());

    private static DivideTranslator instance;

    public static DivideTranslator getInstance() {
        if (instance == null) {
            instance = new DivideTranslator();
        }
        return instance;
    }

    // general ontology prefix
    private static final String META_MODEL_PREFIX =
            "https://divide.idlab.ugent.be/meta-model/divide-core/";

    // general properties
    private static final Property RDF_TYPE_PROPERTY =
            new PropertyImpl("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
    private static final Property QUERY_NAME_PROPERTY =
            new PropertyImpl(META_MODEL_PREFIX + "isTaskForDivideQueryName");
    private static final Property COMPONENT_ID_PROPERTY =
            new PropertyImpl(META_MODEL_PREFIX + "isTaskForComponentId");

    // properties specific to the DIVIDE query location update
    private static final Property UPDATED_LOCATION_PROPERTY =
            new PropertyImpl(META_MODEL_PREFIX + "hasUpdatedQueryLocation");
    private static final Map<String, RspLocation> RSP_LOCATION_MAP = new HashMap<>();
    static {
        RSP_LOCATION_MAP.put("https://divide.idlab.ugent.be/meta-model/divide-core/CentralLocation",
                RspLocation.CENTRAL);
        RSP_LOCATION_MAP.put("https://divide.idlab.ugent.be/meta-model/divide-core/LocalLocation",
                RspLocation.LOCAL);
    }

    // properties specific to the DIVIDE window parameter update
    private static final Property WINDOW_PARAMETER_VARIABLE_PROPERTY =
            new PropertyImpl("http://idlab.ugent.be/sensdesc/window#variable");
    private static final Property WINDOW_PARAMETER_VALUE_PROPERTY =
            new PropertyImpl("http://idlab.ugent.be/sensdesc/window#value");
    private static final Property WINDOW_PARAMETER_TYPE_PROPERTY =
            new PropertyImpl("http://idlab.ugent.be/sensdesc/window#type");
    private static final Property WINDOW_PARAMETER_TIME_DESCRIPTION =
            new PropertyImpl("http://www.w3.org/2006/time#seconds");
    private static final String WINDOW_PARAMETER_LIST_DESCRIPTION_TEMPLATE =
            "@prefix sd: <http://idlab.ugent.be/sensdesc#> .\n" +
                    "@prefix sd-query: <http://idlab.ugent.be/sensdesc/query#> .\n" +
                    "sd-query:pattern sd:correctedWindowParameters ( %s ) .";
    private static final String WINDOW_PARAMETER_DESCRIPTION_TEMPLATE =
            String.format(" [ <%s> \"%s\" ; <%s> %s ; <%s> <%s> ] ",
                    WINDOW_PARAMETER_VARIABLE_PROPERTY, "%s",
                    WINDOW_PARAMETER_VALUE_PROPERTY, "%s",
                    WINDOW_PARAMETER_TYPE_PROPERTY, WINDOW_PARAMETER_TIME_DESCRIPTION);
    private static final Map<String, String> WINDOW_PARAMETER_UPDATE_VARIABLE_MAP = new HashMap<>();
    static {
        WINDOW_PARAMETER_UPDATE_VARIABLE_MAP.put(
                META_MODEL_PREFIX + "hasUpdatedWindowSizeInSeconds", "range");
        WINDOW_PARAMETER_UPDATE_VARIABLE_MAP.put(
                META_MODEL_PREFIX + "hasUpdatedQuerySlidingStepInSeconds", "slide");
    }

    private final Map<String, Task> rdfClassToTaskMap;

    private DivideTranslator() {
        this.rdfClassToTaskMap = new HashMap<>();
        this.rdfClassToTaskMap.put(
                META_MODEL_PREFIX + "DivideQueryLocationUpdateTask",
                Task.QUERY_LOCATION_UPDATE);
        this.rdfClassToTaskMap.put(
                META_MODEL_PREFIX + "DivideWindowParameterUpdateTask",
                Task.WINDOW_PARAMETER_UPDATE);
    }

    public synchronized void translateMessageToDivideAction(IDivideEngine divideEngine,
                                                            String taskInRdf) {
        LOGGER.info("Translating the following RDF message to a task for " +
                "the DIVIDE Engine: {}", taskInRdf);
        LOGGER.info(Constants.METRIC_MARKER,
                "TRANSLATOR_TASK_START\t{}",
                taskInRdf.hashCode());

        if (divideEngine != null) {
            // try to parse the RDF task description
            Model model = JenaUtilities.parseString(taskInRdf, RDFLanguage.TURTLE);
            if (model == null) {
                LOGGER.error("Task description is no valid RDF");
                return;
            }

            // find all resources that represent a task description
            ResIterator iterator = model.listResourcesWithProperty(RDF_TYPE_PROPERTY);
            while (iterator.hasNext()) {
                // retrieve each resource (object) that has the "rdf:type" property,
                // and retrieve the corresponding object of that statement (representing
                // the task individual)
                Resource resource = iterator.next();
                Statement typeStatement = model.getProperty(resource, RDF_TYPE_PROPERTY);
                String taskType = typeStatement.getObject().toString();

                // check if the type corresponds to a DIVIDE engine task
                if (this.rdfClassToTaskMap.containsKey(taskType)) {
                    // retrieve task type
                    Task task = this.rdfClassToTaskMap.get(taskType);
                    LOGGER.info("Task description of type {} found in RDF", task);

                    // retrieve general parameters: component ID & DIVIDE query name
                    Statement componentIdStatement = model.getProperty(
                            typeStatement.getSubject(), COMPONENT_ID_PROPERTY);
                    Statement queryNameStatement = model.getProperty(
                            typeStatement.getSubject(), QUERY_NAME_PROPERTY);
                    if (componentIdStatement == null || queryNameStatement == null) {
                        LOGGER.warn("No component ID and/or query name present in RDF " +
                                "-> task cannot be sent to DIVIDE engine");
                        break;
                    }
                    String componentId = componentIdStatement.getObject().asLiteral().toString();
                    String queryName = queryNameStatement.getObject().asLiteral().toString();

                    if (task == Task.QUERY_LOCATION_UPDATE) {
                        handleLocationUpdate(model, typeStatement,
                                divideEngine, componentId, queryName, taskInRdf);

                    } else if (task == Task.WINDOW_PARAMETER_UPDATE) {
                        handleWindowParameterUpdate(model, typeStatement,
                                divideEngine, componentId, queryName, taskInRdf);
                    }
                }
            }

        } else {
            LOGGER.info("Could not pass the RDF message as a task to the " +
                    "DIVIDE engine since the engine is unknown");
        }
    }

    private void handleLocationUpdate(Model model,
                                      Statement typeStatement,
                                      IDivideEngine divideEngine,
                                      String componentId,
                                      String queryName,
                                      String taskInRdf) {
        // retrieve new location (central or local)
        Statement updatedLocationStatement = model.getProperty(
                typeStatement.getSubject(), UPDATED_LOCATION_PROPERTY);
        if (updatedLocationStatement == null) {
            LOGGER.warn("No updated location present in RDF " +
                    "-> task cannot be sent to DIVIDE engine");
            return;
        }
        boolean moveToCentral;

        Statement queryLocationTypeStatement = model.getProperty(
                updatedLocationStatement.getObject().asResource(), RDF_TYPE_PROPERTY);
        String locationType = queryLocationTypeStatement.getObject().toString();
        RspLocation updatedLocation = RSP_LOCATION_MAP.get(locationType);
        if (updatedLocation == RspLocation.LOCAL) {
            moveToCentral = false;
        } else if (updatedLocation == RspLocation.CENTRAL) {
            moveToCentral = true;
        } else {
            LOGGER.warn("Invalid updated location {} present in RDF " +
                    "-> task cannot be sent to DIVIDE engine", updatedLocation);
            return;
        }

        // enqueue query location update
        try {
            LOGGER.info("Translated RDF description to a location update task " +
                            "for component with ID '{}', DIVIDE query with name '{}', moving to {}",
                    componentId, queryName, updatedLocation);
            LOGGER.info(Constants.METRIC_MARKER,
                    "TRANSLATOR_TASK_END\t{}\t{}\t{}\t{}\t{}",
                    taskInRdf.hashCode(), queryName, componentId,
                    "location_update", moveToCentral ? "central" : "local");
            divideEngine.updateQueryLocation(componentId, queryName, moveToCentral);
        } catch (DivideNotInitializedException e) {
            LOGGER.error("Could not execute DIVIDE query location update", e);
        }
    }

    private void handleWindowParameterUpdate(Model model,
                                             Statement typeStatement,
                                             IDivideEngine divideEngine,
                                             String componentId,
                                             String queryName,
                                             String taskInRdf) {
        // retrieve window parameter description and convert to new Jena model
        StringBuilder updatedWindowParameterList = new StringBuilder();
        // check all available prefixes that represent a new window parameter
        for (String property : WINDOW_PARAMETER_UPDATE_VARIABLE_MAP.keySet()) {
            // retrieve updated window parameter blank node from RDF
            Statement updatedWindowParameterStatement = model.getProperty(
                    typeStatement.getSubject(), new PropertyImpl(property));
            if (updatedWindowParameterStatement != null) {
                // extract number of seconds from statement object
                int seconds = updatedWindowParameterStatement.getObject().asLiteral().getInt();

                // create an RDF/Turtle description of the blank node representing
                // this updated window parameter
                updatedWindowParameterList.append(String.format(
                        WINDOW_PARAMETER_DESCRIPTION_TEMPLATE,
                        WINDOW_PARAMETER_UPDATE_VARIABLE_MAP.get(property),
                        seconds));
            }
        }

        // check if there are actual new window parameters defined
        if (updatedWindowParameterList.toString().trim().isEmpty()) {
            LOGGER.warn("No updated window parameters described in RDF of window parameter" +
                    " update task -> makes no sense to send task to DIVIDE engine");
            return;
        }

        // create an RDF/Turtle string of the full "corrected window parameters"
        // representation required by the DIVIDE engine, and parse it as Jena model
        String updatedWindowParameterDescription = String.format(
                WINDOW_PARAMETER_LIST_DESCRIPTION_TEMPLATE, updatedWindowParameterList);
        Model updatedWindowParameters = JenaUtilities.parseString(
                updatedWindowParameterDescription, RDFLanguage.TURTLE);
        if (updatedWindowParameters == null) {
            LOGGER.warn("Updated window parameter description '{}' in RDF/Turtle cannot be " +
                    "converted to a Jena model due to an RDF syntax error " +
                    "-> task cannot be sent to DIVIDE engine",
                    IOUtilities.removeWhiteSpace(updatedWindowParameterDescription));
            return;
        }

        // enqueue window parameter update
        try {
            LOGGER.info("Translated RDF description to a window parameter update task " +
                            "for component with ID '{}', DIVIDE query with name '{}', " +
                            "and window parameter description: {}",
                    componentId, queryName,
                    IOUtilities.removeWhiteSpace(updatedWindowParameterDescription));
            LOGGER.info(Constants.METRIC_MARKER,
                    "TRANSLATOR_TASK_END\t{}\t{}\t{}\t{}\t{}",
                    taskInRdf.hashCode(), queryName, componentId,
                    "window_parameter_update", updatedWindowParameters.hashCode());
            divideEngine.updateWindowParameters(
                    componentId, queryName, updatedWindowParameters);
        } catch (DivideNotInitializedException e) {
            LOGGER.error("Could not execute DIVIDE query location update", e);
        }
    }



    @SuppressWarnings("unused")
    public static void main(String[] args) {
        String prefixes =
                "@prefix divide-core: <https://divide.idlab.ugent.be/meta-model/divide-core/> .\n" +
                "@prefix monitoring: <https://divide.idlab.ugent.be/meta-model/monitoring/> .\n" +
                "@prefix saref-core: <https://saref.etsi.org/core/> .\n" +
                "@prefix om: <http://www.ontology-of-units-of-measure.org/resource/om-2/> .\n" +
                "@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .\n";

        String windowParameterUpdateTask = prefixes +
                "[ a divide-core:DivideWindowParameterUpdateTask ;\n" +
                "    divide-core:isTaskForDivideQueryName \"query_name\" ;\n" +
                "    divide-core:isTaskForComponentId \"component_id\"^^xsd:string ;\n" +
                "    divide-core:hasUpdatedQuerySlidingStepInSeconds 25 ;\n" +
                "    divide-core:hasUpdatedWindowSizeInSeconds \"50\"^^xsd:integer\n" +
                "] .";

        String queryLocationUpdateTask = prefixes +
                "[ a divide-core:DivideQueryLocationUpdateTask ;\n" +
                "    divide-core:isTaskForDivideQueryName \"query_name\" ;\n" +
                "    divide-core:isTaskForComponentId \"component_id\"^^xsd:string ;\n" +
                "    divide-core:hasUpdatedQueryLocation [ a divide-core:CentralLocation ]\n" +
                "] .";

        DivideTranslator.getInstance().translateMessageToDivideAction(
                DivideEngineFactory.createInstance(), windowParameterUpdateTask);
    }

    private enum Task {
        QUERY_LOCATION_UPDATE,
        WINDOW_PARAMETER_UPDATE
    }

}
