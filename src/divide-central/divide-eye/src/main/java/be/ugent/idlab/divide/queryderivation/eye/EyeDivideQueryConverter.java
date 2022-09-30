package be.ugent.idlab.divide.queryderivation.eye;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.impl.PropertyImpl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class EyeDivideQueryConverter {

    private final EyeDivideQueryDeriver queryDeriver;

    private final Property queryBodyProperty, prefixesProperty,
            declareProperty, prefixProperty, namespaceProperty;

    EyeDivideQueryConverter(EyeDivideQueryDeriver queryDeriver) {
        // save query deriver
        this.queryDeriver = queryDeriver;

        // define property which has the instantiated query bodies as triple object
        this.queryBodyProperty = new PropertyImpl("http://idlab.ugent.be/sensdesc#queryBody");

        // define properties related to the extraction of the query prefixes
        // (note that, since the getQueries method is only called for the output of a
        //  single DIVIDE query, there is always only one set of prefixes defined)
        this.prefixesProperty = new PropertyImpl("http://www.w3.org/ns/shacl#prefixes");
        this.declareProperty = new PropertyImpl("http://www.w3.org/ns/shacl#declare");
        this.prefixProperty = new PropertyImpl("http://www.w3.org/ns/shacl#prefix");
        this.namespaceProperty = new PropertyImpl("http://www.w3.org/ns/shacl#namespace");
    }

    List<String> getQueries(Model model) {
        // iterate over all resources in Jena model that have the query body property
        ResIterator iterator = model.listResourcesWithProperty(queryBodyProperty);

        // define list of queries
        List<String> queries = new ArrayList<>();

        while (iterator.hasNext()) {
            // retrieve each resource (object) that has the query body property,
            // and retrieve the corresponding statement in the query string
            Resource resource = iterator.next();
            Statement queryStatement = model.getProperty(resource, queryBodyProperty);

            // get the object of the statement, which is the query body as string literal
            // -> retrieve this query body
            String query = queryStatement.getObject().asLiteral().toString().trim();

            // retrieve the RSP-QL prefix string to put in front of the query body,
            // to obtain the final RSP-QL query
            query = getPrefixString(model, resource) + query;

            // replace \" by " (this is required for string literals occurring in
            // the query body)
            query = query.replace("\\\"", "\"");

            queries.add(query);
        }

        return queries;
    }

    private String getPrefixString(Model model, Resource resource) {
        // retrieve statement defining the prefix entity of the given query resource
        Statement prefixStatement = model.getProperty(resource, prefixesProperty);
        String prefixesURI = prefixStatement.getObject().toString();

        // check if prefixes URI has already been converted, and only recalculate
        // if this is not the case
        String converted = queryDeriver.retrieveConvertedPrefixesString(prefixesURI);
        if (converted == null) {
            converted = convertPrefixString(model, prefixStatement);
            queryDeriver.saveConvertedPrefixesString(prefixesURI, converted);
        }

        return converted;
    }

    private String convertPrefixString(Model model, Statement prefixStatement) {
        // create iterator over all prefixes declared by the prefix entity
        // of the given query resource
        NodeIterator prefixIterator = model.listObjectsOfProperty(
                prefixStatement.getObject().asResource(), declareProperty);

        StringBuilder prefixesString = new StringBuilder();
        List<String> prefixStrings = new ArrayList<>();
        while (prefixIterator.hasNext()) {
            RDFNode node = prefixIterator.next();

            // for each declared prefix, get the object of both the prefix property
            // (a string literal) and the namespace property (a literal of type xsd:anyURI)
            String prefixName = model.getProperty(node.asResource(), prefixProperty).
                    getObject().toString();
            String prefixURI = model.getProperty(node.asResource(), namespaceProperty).
                    getObject().asLiteral().getValue().toString();

            // create valid RSP-QL string that defines the extracted prefix
            String prefixString = "PREFIX " + prefixName + ": <" + prefixURI + ">\n";

            prefixStrings.add(prefixString);
        }

        // sort array of prefixes to make sure semantically equivalent queries also
        // have an equivalent prefix string
        Collections.sort(prefixStrings);

        // add strings to string builder
        for (String prefixString : prefixStrings) {
            prefixesString.append(prefixString);
        }

        return prefixesString.toString();
    }

}
