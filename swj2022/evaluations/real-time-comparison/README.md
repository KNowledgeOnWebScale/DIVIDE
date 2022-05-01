# Context-aware & privacy-preserving homecare monitoring through adaptive query derivation for IoT data streams with DIVIDE

This folder contains supportive material for the evaluations in the paper "Context-aware & privacy-preserving homecare monitoring through adaptive query derivation for IoT data streams with DIVIDE", which is submitted to the Special Issue on Semantic Web Meets Health Data Management of the Semantic Web Journal.

## Contents

The material in this folder is related to the real-time evaluation of the DIVIDE approach, compared to other alternative approaches that use real-time semantic reasoning. It corresponds to the evaluation set-up and results in the Sections 8.3 and 9.2-3 of the paper ("Real-time evaluation of derived DIVIDE queries").

In concrete, this folder contains the queries that are running on each set-up described in the evaluation set-up in Section 8.3 of the paper. The numbers in the names of the subfolders refer to the evaluation set-up number used in the paper in Section 8.3.1. When a folder contains two query files, the query named `detect_activities` is always executed first, followed by the query named `filter_routine_activities`.

Depending on the set-up, some of them load ontology and/or context files into the reasoning or RSP engine (either RDFox, Jena or C-SPARQL). The context loaded into such engines is the context as defined in [`context.ttl`](../context.ttl) in the upper directory. The ontology files loaded into these same engines are all files in the [`ontology`](../../ontology) folder in the root folder of this repository.

## Contact
 
The main contact person directly involved with this research is [Mathias De Brouwer](https://www.linkedin.com/in/mathiasdebrouwer/). In case of any remarks or questions, you can email [mrdbrouw.DeBrouwer@UGent.be](mailto:mrdbrouw.DeBrouwer@UGent.be) or [create a GitHub issue](../../../../../issues/new).
