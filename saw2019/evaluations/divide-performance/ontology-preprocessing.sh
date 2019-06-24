#!/bin/bash

# load in ontology
eye ../../ontology/CareRoomMonitoring.ttl ../../ontology/RoleCompetenceAccio.ttl ../../ontology/SAREFiot.ttl ../../ontology/SSNiot.ttl ../../ontology/General.ttl ../../ontology/ssnDUL.ttl ../../ontology/uo.ttl ../../ontology/time.ttl ../../ontology/DUL.ttl ../../ontology/ssn.ttl ../../ontology/sosa.ttl ../../ontology/saref.ttl --no-qvars --pass --nope > outputs/ontology.n3

# create OWL-RL rules
eye outputs/ontology.n3 preprocessing/lists.n3 --query preprocessing/instantiate-rules.n3 --nope --no-skolem http://eulersharp.sourceforge.net/.well-known/genid/myVariables > outputs/rules.n3

# create image of EYE reasoner = intermediate code file resulting from Prolog compilation
eye "$@" outputs/ontology.n3 outputs/rules.n3 --image ype.pvm