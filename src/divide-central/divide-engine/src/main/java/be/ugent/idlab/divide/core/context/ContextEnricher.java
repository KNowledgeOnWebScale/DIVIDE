package be.ugent.idlab.divide.core.context;

import be.ugent.idlab.divide.core.engine.DivideOntology;
import be.ugent.idlab.util.rdf.jena3.owlapi4.JenaUtilities;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.rdf.model.InfModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.reasoner.rulesys.GenericRuleReasoner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ContextEnricher implements IContextEnricher {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContextEnricher.class.getName());

    private final List<ContextEnrichingQuery> queries;
    private final ContextEnricherMode mode;
    private final String componentId;

    private DivideOntology registeredOntology;
    private Model baseModel;

    public ContextEnricher(List<ContextEnrichingQuery> queries,
                           ContextEnricherMode mode,
                           String componentId) {
        this.queries = queries;
        this.mode = mode;
        this.componentId = componentId;

        this.registeredOntology = null;
        this.baseModel = ModelFactory.createDefaultModel();
    }

    @Override
    public synchronized void registerOntology(DivideOntology ontology) {
        LOGGER.info("Registering ontology with ID {} to context enricher of component {} with mode {}",
                ontology.getId(), componentId, mode);

        // check if currently registered ontology exists and has the same ID
        // as the new ontology
        if (this.registeredOntology != null &&
                this.registeredOntology.getId().equals(ontology.getId())) {
            // -> if yes, then no action should be taken anymore
            return;
        }

        // update saved ontology to the new ontology
        this.registeredOntology = ontology;

        if (this.queries.isEmpty()) {
            // if no queries are registered for context enrichment, then there is
            // no need to do the ontology registration process
            return;
        }

        if (this.mode == ContextEnricherMode.EXECUTE_ON_CONTEXT_WITHOUT_REASONING ||
                this.mode == ContextEnricherMode.EXECUTE_ON_CONTEXT_WITH_REASONING) {
            // when only executing the queries on the context, nothing should be
            // done with the triples of the registered ontology
            // -> only if reasoning is still done, the rules need to be parsed
            if (this.mode == ContextEnricherMode.EXECUTE_ON_CONTEXT_WITH_REASONING) {
                GenericRuleReasoner reasoner = new GenericRuleReasoner(ontology.getRules());
                // RETE algorithm is required to ensurer fast incremental reasoning
                // -> downside: for some reason, the output of reasoning with this model leads to
                //              duplicate triples in the inferred model (not always the same number)
                reasoner.setMode(GenericRuleReasoner.FORWARD_RETE);

                // already perform reasoning on the model with the ontology triples
                LOGGER.info("Start preparing reasoning during context enrichment with rule reasoner " +
                        "in context enricher of component {}", componentId);
                long start = System.currentTimeMillis();
                InfModel infModel = ModelFactory.createInfModel(
                        reasoner, ModelFactory.createDefaultModel());
                infModel.prepare();
                LOGGER.debug("Finished preparing reasoning during context enrichment with rule reasoner " +
                                "in context enricher of component {} in {} ms",
                        componentId, System.currentTimeMillis() - start);

                // set resulting model as base model for context enrichment
                this.baseModel = infModel;
            }

            return;
        }

        // IF THIS PART IS REACHED: queries definitely need to be executed
        //                          on the ontology triples as well
        // -> add ontology triples to a new Jena model
        Model model = ModelFactory.createDefaultModel();
        model.add(ontology.getModel());

        if (this.mode == ContextEnricherMode.EXECUTE_ON_CONTEXT_AND_ONTOLOGY_WITHOUT_REASONING) {
            // set base model for context enrichment to clean model
            // with only the ontology triples
            this.baseModel = model;

        } else if (this.mode == ContextEnricherMode.EXECUTE_ON_CONTEXT_AND_ONTOLOGY_WITH_REASONING) {
            // convert OWL ontology to a set of Jena rules
            // (i.e., extract all OWL 2 RL axioms from ontology and convert into rules that
            //        Jena understands)
            // -> create a Jena rule reasoner that uses these rules for reasoning
            LOGGER.info("Create Jena rule reasoner with rules extracted from ontology " +
                    "in context enricher of component {}", componentId);
            GenericRuleReasoner reasoner = new GenericRuleReasoner(ontology.getRules());
            // RETE algorithm is required to ensurer fast incremental reasoning
            // -> downside: for some reason, the output of reasoning with this model leads to
            //              duplicate triples in the inferred model (not always the same number)
            reasoner.setMode(GenericRuleReasoner.FORWARD_RETE);

            // already perform reasoning on the model with the ontology triples
            LOGGER.info("Start preparing reasoning during context enrichment with rule reasoner " +
                    "in context enricher of component {}", componentId);
            long start = System.currentTimeMillis();
            InfModel infModel = ModelFactory.createInfModel(reasoner, model);
            infModel.prepare();
            LOGGER.debug("Finished preparing reasoning during context enrichment with rule reasoner " +
                            "in context enricher of component {} in {} ms",
                    componentId, System.currentTimeMillis() - start);

            // set resulting model as base model for context enrichment
            this.baseModel = infModel;
        }
    }

    public synchronized void enrichContext(Context context) {
        long start, end;

        if (queries.isEmpty()) {
            // if no queries are registered, then no context enrichment
            // needs to take place obviously
            LOGGER.info("No queries to enrich context {} for component {}",
                    context.getId(), componentId);

            return;
        }

        LOGGER.info("Enriching context {} for component {}: starting with context of {} triples",
                context.getId(), componentId, context.size());

        // create model for resulting context and add base context
        Model result = ModelFactory.createDefaultModel();
        result.add(context.getContext());

        // add context data to model to execute queries
        start = System.currentTimeMillis();
        this.baseModel.add(context.getContext());
        end = System.currentTimeMillis();
        LOGGER.info("Enriching context {} for component {}: added {} context triples " +
                "to base model (now containing {} triples) in {} ms",
                context.getId(), componentId, context.size(), baseModel.size(), end - start);

        // create model to remove at the end from the base model, and add base context
        Model toBeRemoved = ModelFactory.createDefaultModel();
        toBeRemoved.add(context.getContext());

        // loop over all queries in order
        for (int i = 0; i < queries.size(); i++) {
            ContextEnrichingQuery query = queries.get(i);

            // save model to execute query on
            Model queryModel;
            // -> in the reasoning case, a new query model will be constructed
            //    to remove any duplicates that have been created by the FORWARD_RETE
            //    rule reasoning
            // -> the number of duplicate triples is not deterministic, but (luckily)
            //    the number of unique triples is deterministic!
            // -> so these duplicates need to be removed for the queries
            if (this.mode == ContextEnricherMode.EXECUTE_ON_CONTEXT_AND_ONTOLOGY_WITH_REASONING ||
                    this.mode == ContextEnricherMode.EXECUTE_ON_CONTEXT_WITH_REASONING) {
                start = System.currentTimeMillis();

                // start with empty query model
                queryModel = ModelFactory.createDefaultModel();

                // retrieve the two parts of the inferred model:
                // - the raw model (triples after adding context but before doing incremental
                //   reasoning on new model with added context triples)
                // - the deductions model (triples inferred from doing the incremental reasoning
                //   on new model with added context triples)
                InfModel inferredBaseModel = (InfModel) this.baseModel;
                Model rawModel = inferredBaseModel.getRawModel();
                Model deductionsModel = inferredBaseModel.getDeductionsModel();

                // find duplicates, i.e., triples in deductions model that were already present
                //                        in raw model
                Model duplicates = deductionsModel.intersection(rawModel);

                // create new version of deductions model without the duplicate triples
                Model nonDuplicateDeductionsModel = ModelFactory.createDefaultModel();
                nonDuplicateDeductionsModel.add(deductionsModel);
                nonDuplicateDeductionsModel.remove(duplicates);

                // create query model from original raw model, and deductions model
                // without the duplicate triples
                queryModel.add(rawModel);
                queryModel.add(nonDuplicateDeductionsModel);

                end = System.currentTimeMillis();
                LOGGER.info("Enriching context {} for component {}: removing {} duplicates in " +
                                "{} ms to construct query model with {} triples",
                        context.getId(), componentId,
                        duplicates.size(), end - start, queryModel.size());

            } else {
                // -> in non-reasoning cases, this will be the base model
                queryModel = ModelFactory.createDefaultModel();
                queryModel.add(this.baseModel);
            }

            start = System.currentTimeMillis();
            try (QueryExecution queryExecution =
                         QueryExecutionFactory.create(query.getQuery(), queryModel)) {
                // execute query on query model
                Model queryResult = queryExecution.execConstruct();
                end = System.currentTimeMillis();

                LOGGER.info("Enriching context {} for component {}: executed query {} in " +
                                "{} ms to yield {} additional context triples",
                        context.getId(), componentId, query.getName(),
                        end - start, queryResult.size());
                if (!queryResult.isEmpty()) {
                    JenaUtilities.printModel(queryResult);
                }

                // add resulting triples to context
                result.add(queryResult);

                // add resulting triples to base model to ensure dependent queries work
                // (only if another query follows of course)
                if (i != queries.size() - 1) {
                    LOGGER.info("Temporarily add {} additional context triples resulting from " +
                                    "query {} to base model for execution of following query",
                            queryResult.size(), query.getName());
                    this.baseModel.add(queryResult);
                    toBeRemoved.add(queryResult);
                }

            } catch (Exception e) {
                LOGGER.error("Error during the execution of query {} in context " +
                        "enricher of context {} for component {}",
                        query.getName(), context.getId(), componentId, e);

                // if anything goes wrong during the context enrichment, the original
                // context is returned instead of a partially enriched version
                return;
            }
        }

        // again remove all context data from the model
        start = System.currentTimeMillis();
        this.baseModel.remove(toBeRemoved);
        end = System.currentTimeMillis();
        LOGGER.info("Enriching context {} for component {}: removed context triples " +
                "from base model in {} ms", context.getId(), componentId, end - start);

        // update enriched context
        context.enrichContext(result);
    }

}
