# Context-aware query derivation for IoT data streams with DIVIDE enabling privacy by design

This folder contains supportive material for the evaluations in the paper "Context-aware query derivation for IoT data streams with DIVIDE enabling privacy by design", which is published in the Special Issue on Semantic Web Meets Health Data Management of the Semantic Web Journal.

## Contents

The material in this folder is related to the performance evaluation of the DIVIDE system itself. It corresponds to the evaluation set-up and results in the Sections 8.1 and 9.1 of the paper ("Performance evaluation of DIVIDE").

The [`divide-queries`](divide-queries) subfolder contains the configuration details of the DIVIDE query definitions that are being used in this evaluation. These include the DIVIDE queries corresponding to the toileting and brushing teeth activity rules. For both activities, the root folder of the corresponding DIVIDE query contains the internal representation of this DIVIDE query. For the toileting query, the end-user definition (as a series of SPARQL queries) is also included in the `sparql` subfolder.

Note that the DIVIDE query for the showering activity (used in the real-time comparison evaluation) is the same DIVIDE query as the one for the toileting query (as explained in the paper).

The [`configuration`](configuration) subfolder contains the configuration files to run this evaluation. Note that these configuration files expect the ontology & query files to be in the correct subdirectories, relative to the location of the directory that contains these configuration files & the DIVIDE server JAR.

To actually run this evaluation, we have run the DIVIDE server JAR with the given configuration files (properties JSON file and components CSV file) and then fed the evaluation's [`context.ttl`](../context.ttl) file to the `homelab_patient` ABox of the Knowledge Base server. More details about how to do this are given in the general README.md file of the [DIVIDE source code folder](../../../src/divide-central) of this repository.

## Contact
 
The main contact person directly involved with this research is [Mathias De Brouwer](https://www.linkedin.com/in/mathiasdebrouwer/). In case of any remarks or questions, you can email [mrdbrouw.DeBrouwer@UGent.be](mailto:mrdbrouw.DeBrouwer@UGent.be) or [create a GitHub issue](../../../../../issues/new).
