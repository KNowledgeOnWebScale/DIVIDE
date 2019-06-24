# DIVIDE: Adaptive Context-Aware Query Derivation for IoT Data Streams

This folder contains the ontology data related to the running healthcare example used in the paper "DIVIDE: Adaptive Context-Aware Query Derivation for IoT Data Streams", which is submitted to the 1st International Workshop on Sensors and Actuators on the Web ([SAW2019](http://saw.gitlab.emse.fr/2019/)) workshop at [ISWC 2019](https://iswc2019.semanticweb.org/). This paper is currently under review.

## Contents

In the paper, the DIVIDE system is explained through a running healthcare example. This folder contains all ontology files related to this example.

The used ontology is a snapshot of the [ACCIO continuous care ontology](https://github.com/IBCNServices/Accio-Ontology/tree/gh-pages). The main ontology file is [`CareRoomMonitoring.ttl`](CareRoomMonitoring.ttl). All direct and indirect imports of this ontology are also included in this folder.

Consider the list below for a mapping of the ontology files in this folder to the URI of the ontology they contain. Note that this folder contains all ontologies in Turtle format.

* `CareRoomMonitoring.ttl`: http://IBCNServices.github.io/Accio-Ontology/DIVIDE-example/CareRoomMonitoring.owl (this ontology is not hosted yet at the provided URL)
* `RoleCompetenceAccio.ttl`: http://IBCNServices.github.io/Accio-Ontology/RoleCompetenceAccio.owl
* `General.ttl`: http://IBCNServices.github.io/Accio-Ontology/General.owl
* `SAREFiot.ttl`: http://IBCNServices.github.io/Accio-Ontology/SAREFiot.owl
* `SSNiot.ttl`: http://IBCNServices.github.io/Accio-Ontology/SSNiot.owl
* `sosa.ttl`: http://www.w3.org/ns/sosa/
* `saref.ttl`: https://w3id.org/saref
* `ssn.ttl`: http://www.w3.org/ns/ssn/
* `ssnDUL.ttl`: http://IBCNServices.github.io/Accio-Ontology/ontologies/ssnDUL.owl
* `DUL.ttl`: http://IBCNServices.github.io/Accio-Ontology/ontologies/DUL.owl
* `time.ttl`: http://www.w3.org/2006/time
* `uo.ttl`: http://IBCNServices.github.io/Accio-Ontology/ontologies/uo.owl

This ontology is also used for the paper's [evaluations](../evaluations).

## Contact
 
The main contact person directly involved with this research is [Mathias De Brouwer](https://www.linkedin.com/in/mathiasdebrouwer/). In case of any remarks or questions, you can send an email to [mrdbrouw.DeBrouwer@UGent.be](mailto:mrdbrouw.DeBrouwer@UGent.be) or [create a GitHub issue](../../../../../issues/new).