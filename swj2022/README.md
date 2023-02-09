# Context-aware query derivation for IoT data streams with DIVIDE enabling privacy by design

This folder contains all supportive material related to the paper "Context-aware query derivation for IoT data streams with DIVIDE enabling privacy by design", which is published in the Special Issue on Semantic Web Meets Health Data Management of the Semantic Web Journal.

## Contents

In the paper, DIVIDE is explained through a running homecare monitoring example. This example is also used for the paper's evaluations.

This folder contains three subfolders:

* [`ontology`](ontology): This folder contains all files of the Activity Recognition ontology, used for the running homecare monitoring example, that is discussed in the paper. This is a snapshot of the [DAHCC ontology](https://github.com/predict-idlab/DAHCC-Sources), complemented with an ontology file [`KBActivityRecognition.ttl`](ontology/KBActivityRecognition.ttl) that represents all extra definitions related to the knowledge-driven activity recognition. All files in this folder together represent the Activity Recognition ontology. This ontology is also used for the paper's evaluations.
* [`evaluations`](evaluations): This folder contains supportive material related to the evaluations performed in the paper.
* [`eye-implementation`](eye-implementation): This folder contains some more details concerning the implementation of the initialization and query derivation of DIVIDE with the EYE reasoner.

Moreover, this folder contains the different versions of the paper that have been submitted to the Special Issue on Semantic Web Meets Health Data Management of the Semantic Web Journal.
* [paper_v1_submitted_2022-05-01.pdf](paper_v1_submitted_2022-05-01.pdf): This PDF represents the original version of the paper, that was submitted on 1 May 2022. It contains additional details about the DIVIDE methodology and the use case scenario, that have been removed in the first revision of the paper.
* [paper_v2_submitted_2022-09-30.pdf](paper_v2_submitted_2022-09-30.pdf): This PDF represents the revised version of the paper, that was submitted on 30 September 2022 and accepted on 12 January 2023.

The implementation of the DIVIDE central services can be found in the [src/divide-central](../src/divide-central) folder of this repository.

## Contact
 
The main contact person directly involved with this research is [Mathias De Brouwer](https://www.linkedin.com/in/mathiasdebrouwer/). In case of any remarks or questions, you can email [mrdbrouw.DeBrouwer@UGent.be](mailto:mrdbrouw.DeBrouwer@UGent.be) or [create a GitHub issue](../../../issues/new). 
