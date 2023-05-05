# DIVIDE: Adaptive Context-Aware Query Derivation for IoT Data Streams

This folder contains supportive material for the evaluations in the paper "DIVIDE: Adaptive Context-Aware Query Derivation for IoT Data Streams", which is accepted to the 1st International Workshop on Sensors and Actuators on the Web (SAW2019) at [ISWC 2019](https://iswc2019.semanticweb.org/).

## Contents

The material in this folder is related to the evaluation of the real-time processing times of the DIVIDE approach, compared to other alternative approaches that use real-time reasoning. It corresponds to the evaluation set-up and results in the Sections 4.2 and 5.2 of the paper ("Comparison of DIVIDE with Real-Time Reasoning Approaches").

In concrete, this folder contains the queries that are running on each set-up described in the evaluation set-up in Section 4.2 of the paper.

1. **DIVIDE approach using C-SPARQL without reasoning ([`DIVIDE-with-CSPARQL`](DIVIDE-with-CSPARQL)):** both the `FilterLightIntensity` and `FilterSound` queries are running on the C-SPARQL engine. Note that these queries are exactly the [queries outputted by the query derivation in the DIVIDE performance evaluation](../divide-performance/outputs/substituted-queries.n3), after translation from RSP-QL to C-SPARQL syntax.

2. **StreamFox ([`StreamFox`](StreamFox)):** The queries running on the RDFox engine are (in order of execution):
    * `Q1_FilterSymptoms.query`: this query is referred to as "query 1" in the paper
    * `Q2_ForwardActions.query`: this query is referred to as "query 2" in the paper

3. **C-SPARQL piped with (non-streaming) RDFox ([`Pipe-CSPARQL-RDFox`](Pipe-CSPARQL-RDFox)):**
    * On C-SPARQL, the query `Q1_FilterSymptoms.csparql` is running. This query is the same as query 1 in the StreamFox set-up, but modified to a valid C-SPARQL query.
    * On RDFox, the query `Q2_ForwardActions.query` is running. This query is exactly the same as query 2 in the StreamFox set-up.

The context loaded into the engines (RDFox in StreamFox set-up, and both C-SPARQL and RDFox in the piped set-up) is at least the context as defined in [`context.ttl`](context.ttl), potentially complemented with additional sensors (this file only contains 1 light sensor). The ontology files loaded into these same engines are all files in the [`ontology`](../../ontology) folder.

## Contact
 
The main contact person directly involved with this research is [Mathias De Brouwer](https://www.linkedin.com/in/mathiasdebrouwer/). In case of any remarks or questions, you can email [mrdbrouw.DeBrouwer@UGent.be](mailto:mrdbrouw.DeBrouwer@UGent.be) or [create a GitHub issue](../../../../../issues/new).
