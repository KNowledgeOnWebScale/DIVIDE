# DIVIDE Meta Model ontology

This folder contains the DIVIDE Meta Model ontology, which is described in the paper "Enabling efficient semantic stream processing across the IoT network through adaptive distribution with DIVIDE". [This page](../jnsm2023) of this repository is dedicated to this paper.

## Contents

This folder contains all files of the DIVIDE Meta Model ontology. This includes the two main modules of the ontology:

- [`DivideCore.owl`](DivideCore.owl): this module contains all constructs that allow modeling meta-information about DIVIDE.
- [`Monitoring.owl`](Monitoring.owl): this module allows representing
the properties monitored by the DIVIDE Local Monitor and their observations. It builds further on the `DivideCore` module by importing it.

In addition, this folder also contains the three existing ontologies that are imported by the `DivideCore` module of the Meta Model ontology:

- [`saref.ttl`](saref.ttl): the Smart Applications REFerence (SAREF) ontology
- [`om-2.0.ttl`](om-2.0.ttl): the Ontology of units of Measure (OM)
- [`denon_ng.ttl`](denon_ng.ttl): the DEN-ng model (a semantic model for the management of computer networks) - mainly used as inspiration but also imported for reference purposes

## Contact

The main contact person directly involved with this research is [Mathias De Brouwer](https://www.linkedin.com/in/mathiasdebrouwer/). In case of any remarks or questions, you can email [mrdbrouw.DeBrouwer@UGent.be](mailto:mrdbrouw.DeBrouwer@UGent.be) or [create a GitHub issue](../../../issues/new). 
