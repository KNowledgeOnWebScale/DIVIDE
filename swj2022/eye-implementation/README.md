# Context-aware query derivation for IoT data streams with DIVIDE enabling privacy by design

This folder contains DIVIDE implementation details related to the paper "Context-aware query derivation for IoT data streams with DIVIDE enabling privacy by design", which is submitted to the Special Issue on Semantic Web Meets Health Data Management of the Semantic Web Journal.

## Contents

This folder contains some more details concerning the implementation of the initialization and query derivation of DIVIDE with the EYE reasoner.

DIVIDE is implemented in Java and available as a set of Java modules. The initialization and query derivation of DIVIDE contains different steps and actions that are implemented with the EYE reasoner. To execute these actions, the Java implementation calls the EYE reasoner installation. This folder contains exactly these calls, but represented as bash shell commands in a shell script. These scripts can be executed to do exactly the same operations as done during the initialization or query derivation by the Java implementation of DIVIDE.

In concrete, two scripts are provided:

- [`ontology-preprocessing.sh`](ontology-preprocessing.sh): This script performs the ontology preprocessing done by the EYE reasoner during the initialization phase of DIVIDE.
- [`query-derivation.sh`](query-derivation.sh): This script performs the steps performed by the EYE reasoner during the query derivation, for a given DIVIDE query. As discussed in Sections 6 and 7 of the paper, these staps are the semantic reasoning, query extraction, input variable substitution and window parameter substitution. Hence, this script assumes that the context used as input for the query derivation is already enriched. The output of the script is stored in the `output/<DIVIDE_QUERY>/substituted-queries.n3` file and contains the N3 representation of the derived RSP-QL queries.
In this folder, the script can be run for two DIVIDE queries: the one corresponding to the toileting and showering activity rules, and the one corresponding to the brushing teeth activity rule. For these examples, the evaluation context in [`context.ttl`](../evaluations/context.ttl) is used. This forms no problem with respect to the skipping of the context enrichment, since no context-enriching queries are defined for these DIVIDE queries.

The comments in the script files give additional explanation on what is happening. Both scripts use the resources in the `preprocessing` and `query-derivation` folders. They store their output in the `output` directory. Currently, this directory contains the output of running the following scripts:

```
./ontology-preprocessing.sh
./query-derivation.sh query-toileting-showering
./query-derivation.sh query-brushing-teeth
```

## Contact
 
The main contact person directly involved with this research is [Mathias De Brouwer](https://www.linkedin.com/in/mathiasdebrouwer/). In case of any remarks or questions, you can email [mrdbrouw.DeBrouwer@UGent.be](mailto:mrdbrouw.DeBrouwer@UGent.be) or [create a GitHub issue](../../../../issues/new).
