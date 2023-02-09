# Context-aware query derivation for IoT data streams with DIVIDE enabling privacy by design

This folder contains the ontology data related to the paper "Context-aware query derivation for IoT data streams with DIVIDE enabling privacy by design", which is published the Special Issue on Semantic Web Meets Health Data Management of the Semantic Web Journal.

## Contents

In the paper, DIVIDE is explained through a running homecare monitoring example. This folder contains all files of the Activity Recognition ontology that is being used for this example.

The Activity Recognition ontology contains two parts:

- A snapshot of the [DAHCC ontology](https://github.com/predict-idlab/DAHCC-Sources). This ontology contains definitions to perform Data Analytics in Health and Connected Care. More information about the DAHCC ontology is available via [this website](https://dahcc.idlab.ugent.be). The files used for this paper are:
  - The general ontology files included in the `Ontology` folder of the DAHCC GitHub repo. Note that the current repository contains the RDF/Turtle representation of these ontology files.
  - The TBox definitions extracted from the `_Homelab.owl` and `_HomelabWearable.owl` files in the `instantiated_examples` folder of the DAHCC GitHub repo (in RDF/Turtle format).
  - All imports of the `imports` folder of the DAHCC GitHub repo that are being (indirectly) imported by any of the other included DAHCC files.
- The additional ontology file [`KBActivityRecognition.ttl`](KBActivityRecognition.ttl) that represents all extra definitions related to the knowledge-driven activity recognition.

All files in this folder together represent the Activity Recognition ontology. 
This ontology is also used for the paper's [evaluations](../evaluations).

## Contact
 
The main contact person directly involved with this research is [Mathias De Brouwer](https://www.linkedin.com/in/mathiasdebrouwer/). In case of any remarks or questions, you can email [mrdbrouw.DeBrouwer@UGent.be](mailto:mrdbrouw.DeBrouwer@UGent.be) or [create a GitHub issue](../../../../issues/new).
