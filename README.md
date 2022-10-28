# DIVIDE: Context-aware query derivation for IoT data streams enabling privacy by design

This repository contains code and data related to DIVIDE. DIVIDE can serve as a component of a semantic IoT platform. The main goal of DIVIDE is to automatically derive queries for an IoT platform's stream processing components, which filter the IoT data streams. This happens in a context-aware, adaptive way. Whenever the application context changes, DIVIDE derives the queries that filter the observations of interest for the use case, based on this new or changed context. By performing the reasoning upon context changes, relevant sensors and their observations can be filtered in real-time without the need to perform any more reasoning.

This research is executed at the [IDLab](http://idlab.technology) research group, by people from [Ghent University](https://www.ugent.be/en) – [imec](https://www.imec-int.com/en/home).

## Contents

This repository contains the following folders:

- [`saw2019`](saw2019): This folder contains supporting material for the first paper presenting the DIVIDE system. This paper has been published through the 1st International Workshop on Sensors and Actuators on the Web ([SAW2019](http://saw.gitlab.emse.fr/2019/)) at [ISWC 2019](https://iswc2019.semanticweb.org/). This paper can be found [here](http://ceur-ws.org/Vol-2549/article-01.pdf).
- [`swj2022`](swj2022): This folder contains supporting material for a journal paper about DIVIDE, submitted to the Special Issue on Semantic Web Meets Health Data Management of the [Semantic Web Journal](https://www.semantic-web-journal.net/). This paper is currently under review. The most recently submitted version for this paper can be found [here](https://www.semantic-web-journal.net/content/context-aware-query-derivation-iot-data-streams-divide-enabling-privacy-design).
- [`src/divide-central`](src/divide-central): This folder contains the source code of the DIVIDE central services. 

## Contact
 
The main contact person directly involved with this research is [Mathias De Brouwer](https://www.linkedin.com/in/mathiasdebrouwer/). In case of any remarks or questions, you can send an email to [mrdbrouw.DeBrouwer@UGent.be](mailto:mrdbrouw.DeBrouwer@UGent.be) or [create a GitHub issue](../../issues/new).
