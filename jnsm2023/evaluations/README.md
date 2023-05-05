# Enabling efficient semantic stream processing across the IoT network through adaptive distribution with DIVIDE

This folder contains supportive material for the evaluations in the paper "Enabling efficient semantic stream processing across the IoT network through adaptive distribution with DIVIDE", which is submitted for review to the [Journal of Network and Systems Management](https://www.springer.com/journal/10922).

## Contents

The folder contains supportive material for the evaluations performed in the paper. More specifically, it contains the following files and folders:

- [`ontology`](ontology): This folder contains the ontology used for the paper's evaluations. This ontology consists of a snapshot of the [Data Analytics for Health and Connected Care (DAHCC) ontology](https://github.com/predict-idlab/DAHCC-Sources), complemented with an additional module ([`KBActivityRecognition.ttl`](ontology/KBActivityRecognition.ttl)) that represents all extra definitions related to the health parameter calculator system (which includes the definitions related to the calculation of a person's activity index).
- [`context.ttl`](context.ttl): This file represents the context used for the evaluations. This file contains all context triples in RDF/Turtle syntax.
- [`divide-query`](divide-query): This folder contains the internal representation of the DIVIDE query that can be instantiated to an RSP query that calculates a patient's activity index. This DIVIDE query is registered to the DIVIDE engine for all evaluation scenarios of the paper.
- [`global-monitor-queries`](global-monitor-queries): This folder contains the different SPARQL queries that are configured as Global Monitor queries in the evaluation scenarios of this paper.
- [`configuration`](configuration): This folder contains the configuration files to run the different evaluation scenarios. Note that these configuration files expect the ontology & query files to be in the correct subdirectories, relative to the location of the directory that contains these configuration files & the DIVIDE Central executable JAR. The configuration files are provided for the two evaluation scenarios:
  - [`scenario-1-window-parameters`](configuration/scenario-1-window-parameters): This folder contains the configuration files for evaluation scenario 1 (updating the RSP query window parameters based on RSP monitoring).
  - [`scenario-2-query-location`](configuration/scenario-2-query-location): This folder contains the configuration files for evaluation scenario 2 (updating the RSP query location based on network monitoring). The paper also reports additional results for this second evaluation scenario, specifically for the DIVIDE Monitoring set-up. The only difference to the configuration files is updating the network round trip time thresholds in the global monitor queries. In the configuration, this means that the suffixes of the query paths in the `monitor.task_queries` property of the `divide-baseline.properties.json` file are updated from `_medium` to either `_low` and `_high`.

The [`divide-server-1.0-jar-with-dependencies.jar`](divide-server-1.0-jar-with-dependencies.jar) file represents the compiled Java JAR of the DIVIDE server module (of DIVIDE Central) used for the evaluations in the paper. The version of the Local Monitor used in the evaluations is represented by the [`divide-local-monitor-1.0-jar-with-dependencies.jar`](divide-local-monitor-1.0-jar-with-dependencies.jar) file. The corresponding source code can be found in the [`src`](../../src) folder of this repository. The README file of this folder contains detailed information and an example on how to run the DIVIDE Central (server) JAR and the DIVIDE Local Monitor JAR. The version of the source code to build the given Java JAR files (and thus the version used for the evaluations) is tagged with the 'jnsm-2023' tag (see [tag page](../../../../tags)).

The realistic dataset, collected in the imec-UGent HomeLab and used in the paper to create the simulation dataset, is publicly available on the DAHCC ontology website via [this link](https://dahcc.idlab.ugent.be/dataset.html).

## Contact
 
The main contact person directly involved with this research is [Mathias De Brouwer](https://www.linkedin.com/in/mathiasdebrouwer/). In case of any remarks or questions, you can email [mrdbrouw.DeBrouwer@UGent.be](mailto:mrdbrouw.DeBrouwer@UGent.be) or [create a GitHub issue](../../../../issues/new).
