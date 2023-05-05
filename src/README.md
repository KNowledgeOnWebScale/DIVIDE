# Implementation of DIVIDE

This folder contains the implementation details of DIVIDE. This is a first implementation of the methodological design that is presented in the two journal publications about DIVIDE (of which more details can be found in the [`../swj2022`](../swj2022) and [`../jnsm2023`](../jnsm2023) folders of this repository).

***Important:** It should be noted that the implementation in this folder is a first Proof-of-Concept implementation of DIVIDE. This means that the implementation has not been automatically tested yet. It has been tested manually for the purpose of the evaluations in the corresponding papers and usage of the component in research projects. Hence, several implementation improvements should be performed to make the system more robust and complete. These are further detailed in the paper corresponding to the [`../jnsm2023`](../jnsm2023) folder.*

## Contents

This repository contains two main subfolders: one for the implementation of DIVIDE Central ([`divide-central`](divide-central)), and one for the implementation of the DIVIDE Local Monitor ([`divide-local-monitor`](divide-local-monitor)). These two subcomponents are detailed on the figure below, which presents the overall architecture of a typical cascading reasoning set-up in an IoT network, in which DIVIDE should be deployed.

![Cascading reasoning architecture with DIVIDE](../DIVIDE-cascading-architecture.png)

The implementation of both DIVIDE Central and the DIVIDE Local Monitor is done in Java. Both subcomponents of DIVIDE are compiled through Apache Maven (via a `pom.xml` file) into an executable Java JAR file. The subfolders contain these executable Java JAR files, corresponding to the current version of the code.

### Implementation of DIVIDE Central

The [`divide-central`](divide-central) folder contains the details of the implementation of DIVIDE Central. The corresponding Maven project contains multiple submodules:

- `divide-engine`: This module contains the core logic of DIVIDE. It is responsible for many tasks, which include (bot are not limited to): initializing DIVIDE, parsing and managing all DIVIDE queries, managing the DIVIDE components, maintaining a task queue for the DIVIDE components and performing the different DIVIDE engine tasks (e.g., query derivation, window parameter update, query location update), alerting relevant updates to the DIVIDE Meta Model (if enabled), managing the queries on the different stream processing engines.
- `divide-api`: This module contains details about the REST API to interact with the DIVIDE engine. This REST API allows adding, deleting and requesting information about DIVIDE queries and DIVIDE components.
- `divide-query-derivation`: This module is used by the DIVIDE engine to select the correct reasoner and logic used for the ontology preprocessing and DIVIDE query derivation. It is added to enable the integration of other semantic reasoners in the future (if this would ever be desired).
- `divide-eye`: This module contains the implementation of the ontology preprocessing and query derivation steps. This implementation uses the EYE reasoner, which is an N3 reasoner that runs in a Prolog virtual machine.
- `divide-global-monitor`: This module contains the implementation of the DIVIDE Global Monitor (which also includes the implementation of the DIVIDE Meta Model as part of the Global Monitor Reasoning Service) and the DIVIDE Monitor Translator.
- `divide-server`: This module puts all other modules together into a server that starts up DIVIDE Central. This module contains the entry point of the executable Java JAR that is created when compiling the Maven project of DIVIDE Central. It reads in the configuration files, and initializes the DIVIDE engine, the DIVIDE queries and components, the DIVIDE REST API, and the DIVIDE Global Monitor and Local Monitor instances (if enabled).

All modules except for `divide-global-monitor` together represent the implementation of DIVIDE Core.

The executable Java JAR file that should be used to start DIVIDE Central is available as `divide-server-1.0-jar-with-dependencies.jar`.

### Implementation of the DIVIDE Local Monitor

The [`divide-local-monitor`](divide-local-monitor) folder contains the details of the implementation of the DIVIDE Local Monitor. The corresponding Maven project consists of a single module. This module includes the implementation of the Local Monitor RSP Engine, the Semantic Meta Mapper, and the different individual monitors (Device Monitor, Network Monitor, RSP Engine Monitor).

The executable Java JAR file that should be used to start the DIVIDE Local Monitor is available as `divide-local-monitor-1.0-jar-with-dependencies.jar`.

## How to configure and run DIVIDE

When using DIVIDE, everything is managed from within the single executable JAR of DIVIDE Central (the DIVIDE server JAR). This JAR file will manage all subcomponents of DIVIDE, also the instances of the DIVIDE Local Monitor on other devices in the network (if enabled). For this, the implementation uses SSH and expects that every DIVIDE
component in the network is reachable and allows incoming SSH connections using SSH public key authentication with a predefined username (`divide` in the current implementation).

This section of the README will zoom in on how to run DIVIDE, with some pointers to example configurations that can be used as a starting point when running DIVIDE Central on your own. In addition, some details of the DIVIDE Local Monitor are provided as well (even though this should in fact not be managed by the end user).

### DIVIDE Central

To start DIVIDE Central, the executable Java JAR of the `divide-server` submodule of the `divide-central` Maven project should be run. This can be done with the following template Bash command:

```
java -cp divide-central/divide-server-1.0-jar-with-dependencies.jar \
     be.ugent.idlab.divide.DivideServer \
     <properties_file> <components_file>
```

In case additional libraries should be added to the classpath (e.g. to use a specific type of knowledge base), this library folder should be added to the classpath.

The following config files should be used in the template command:

- `<properties_file>`: This is the main config file of DIVIDE Central. For this, a JSON file should be created with all properties of the system. It defines details about the knowledge base (*if* it is deployed by the DIVIDE server), the ontology, the reasoner and engine, the DIVIDE queries, the hosted server, the DIVIDE monitoring, and a central RSP engine used by DIVIDE for hosting central queries.
- `<components_file>` To specify the DIVIDE components, an additional CSV file should be provided. Every entry in this file represents a single DIVIDE component.

Details about the expected inputs of the config files can be found in the respective journal publications (more specifically in the "Implementation" sections of those papers). For the JSON properties file, details about the properties (available properties, whether they are required, default values in case they are not required) are added to the documentation of the source code (and more specifically the [package responsible for documentation](divide-central/divide-server/src/main/java/be/ugent/idlab/divide/configuration).

Examples of possible config files (both the JSON property file and CSV components file) are provided for the evaluations performed in the different journal publications about DIVIDE (see [`../swj2022/evaluations/divide-performance/configuration`](../swj2022/evaluations/divide-performance/configuration) and [`../jnsm2023/evaluations/configuration`](../jnsm2023/evaluations/configuration) folders of this repository). These examples can be used to start DIVIDE Central in an IoT network, provided that the details are updated to the network at hand (e.g., the IP addresses in the config files should probably be updated, etc.). Note that the first journal publication about DIVIDE ([`../swj2022`](../swj2022) folder) did not yet include the DIVIDE monitor & central RSP engine properties in the JSON config file, as these were added later. For backwards compatibility, omitting the related properties from the JSON config file will not cause any issues as these properties are not required and the monitor will be disabled by default if the properties are not provided.

### DIVIDE Local Monitor

When using DIVIDE with the monitoring enabled, DIVIDE Central will configure, deploy and manage the DIVIDE Local Monitor instances on the different DIVIDE components in the IoT network. Hence, as an end user, you should never configure and run the DIVIDE Local Monitor. However, to play around with it and see how it works, you could also start up the DIVIDE Local Monitor on your own. To this end, the following Bash command should be used:

```
java -jar divide-local-monitor/divide-local-monitor-1.0-jar-with-dependencies.jar \
    <properties_file>
```

An example configuration to be used in the file path of the JSON `<properties_file>` is the following:

```json
{
  "component_id": "10.10.145.9-8175-",
  "device_id": "10.10.145.9",
  "monitor": {
    "rsp": true,
    "network": true,
    "device": true
  },
  "local": {
    "rsp_engine": {
      "monitor": {
        "ws_port": 54548
      }
    },
    "public_network_interface": "wlp1s0"
  },
  "central": {
    "monitor_reasoning_service": {
      "protocol": "http",
      "host": "10.10.145.233",
      "port": 54555,
      "uri": "/globalmonitorreasoningservice"
    }
  }
}
```

Note that you should update this configuration with the correct properties of the Global Monitor Reasoning Service, the IP address of the device in the ID, the public network interface and the details of the Local RSP Engine. Once again, note that this full config file will be constructed automatically by DIVIDE Central when actually using DIVIDE.

## Contact
 
The main contact person directly involved with this research is [Mathias De Brouwer](https://www.linkedin.com/in/mathiasdebrouwer/). In case of any remarks or questions, you can email [mrdbrouw.DeBrouwer@UGent.be](mailto:mrdbrouw.DeBrouwer@UGent.be) or [create a GitHub issue](../../../issues/new).
