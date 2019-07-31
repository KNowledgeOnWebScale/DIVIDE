# DIVIDE: Adaptive Context-Aware Query Derivation for IoT Data Streams

This folder contains supportive material for the evaluations in the paper "DIVIDE: Adaptive Context-Aware Query Derivation for IoT Data Streams", which is accepted to the 1st International Workshop on Sensors and Actuators on the Web ([SAW2019](http://saw.gitlab.emse.fr/2019/)) at [ISWC 2019](https://iswc2019.semanticweb.org/).

## Contents

The material in this folder is related to the performance evaluation of the DIVIDE system itself. It corresponds to the evaluation set-up and results in the Sections 4.1 and 5.1 of the paper ("DIVIDE Performance Evaluation").

The folder contains the different scripts that relate to the ontology preprocessing and query derivation processes explained in the paper. Both processes are evaluated on the healthcare example used within the paper. All outputs of the scripts are stored in the [`outputs`](outputs) folder.

- [`ontology-preprocessing.sh`](ontology-preprocessing.sh): This script performs the ontology preprocessing. It uses the ontology data in the [ontology folder](../../ontology). The main output of this script is `ype.pvm` file, which is the image of the EYE reasoner compiled with Prolog, that has loaded the full ontology and ontology-specific rules.

- [`query-derivation.sh`](query-derivation.sh): This script performs the actual query derivation. It uses the `ype.pvm` image created by the ontology preprocessing script. It produces an output in each step of the process:
    1. the EYE proof towards the goal ([`proof.n3`](outputs/proof.n3))
    2. the queries extracted from the proof ([`extracted-queries.n3`](outputs/extracted-queries.n3))
    3. the instantiated queries with substituted input variables ([`substituted-queries.n3`](outputs/substituted-queries.n3)).

## Contact
 
The main contact person directly involved with this research is [Mathias De Brouwer](https://www.linkedin.com/in/mathiasdebrouwer/). In case of any remarks or questions, you can send an email to [mrdbrouw.DeBrouwer@UGent.be](mailto:mrdbrouw.DeBrouwer@UGent.be) or [create a GitHub issue](../../../../../issues/new).
