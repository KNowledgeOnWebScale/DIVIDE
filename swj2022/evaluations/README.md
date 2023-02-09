# Context-aware query derivation for IoT data streams with DIVIDE enabling privacy by design

This folder contains supportive material for the evaluations in the paper "Context-aware query derivation for IoT data streams with DIVIDE enabling privacy by design", which is published in the Special Issue on Semantic Web Meets Health Data Management of the Semantic Web Journal.

## Contents

The folder contains supportive material for the following evaluations:

* [`divide-performance`](divide-performance): The material in this folder is related to the performance evaluation of DIVIDE. It corresponds to the evaluation set-up and results in the Sections 8.1 and 9.1 of the paper ("Performance evaluation of DIVIDE").
* [`real-time-comparison`](real-time-comparison): The material in this folder is related to the real-time evaluation of the DIVIDE approach, compared to other alternative approaches that use real-time semantic reasoning. It corresponds to the evaluation set-up and results in the Sections 8.3 and 9.2-3 of the paper ("Real-time evaluation of derived DIVIDE queries").

The context used for the evaluations is represented by the [`context.ttl`](context.ttl) file. This file contains all context triples in RDF/Turtle syntax.

The [`divide-server-1.0-jar-with-dependencies.jar`](divide-server-1.0-jar-with-dependencies.jar) file represents the compiled Java JAR of the DIVIDE server module used for the evaluations in the paper. The corresponding source code can be found in the [`src/divide-central`](../../src/divide-central) folder of this repository. The README file of this folder contains detailed information and an example on how to run the DIVIDE server JAR. The version of the source code to build the given Java JAR (and thus the version used for the evaluations) is tagged with the 'swj-2022' tag (see [tag page](../../../../tags)).

The realistic dataset, collected in the imec-UGent HomeLab and used in the paper to extract the activity rules for this evaluation and to create the simulation dataset, is publicly available on the DAHCC ontology website via [this link](https://dahcc.idlab.ugent.be/dataset.html).

## Contact
 
The main contact person directly involved with this research is [Mathias De Brouwer](https://www.linkedin.com/in/mathiasdebrouwer/). In case of any remarks or questions, you can email [mrdbrouw.DeBrouwer@UGent.be](mailto:mrdbrouw.DeBrouwer@UGent.be) or [create a GitHub issue](../../../../issues/new).
