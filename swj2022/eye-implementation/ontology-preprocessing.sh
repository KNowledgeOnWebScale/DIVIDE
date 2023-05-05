#!/bin/bash

DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )

# 1. Read in all ontology files with EYE, and collect them in a single N3 file
eye "$DIR"/../ontology/ontology-no-imports/KBActivityRecognition.ttl \
    "$DIR"/../ontology/ontology-no-imports/ActivityRecognition.ttl \
    "$DIR"/../ontology/ontology-no-imports/MonitoredPerson.ttl \
    "$DIR"/../ontology/ontology-no-imports/Sensors.ttl \
    "$DIR"/../ontology/ontology-no-imports/SensorsAndActuators.ttl \
    "$DIR"/../ontology/ontology-no-imports/SensorsAndWearables.ttl \
    "$DIR"/../ontology/ontology-no-imports/_Homelab_tbox.ttl \
    "$DIR"/../ontology/ontology-no-imports/_HomelabWearable_tbox.ttl \
    "$DIR"/../ontology/ontology-no-imports/imports/affectedBy.ttl \
    "$DIR"/../ontology/ontology-no-imports/imports/cpannotationschema.ttl \
    "$DIR"/../ontology/ontology-no-imports/imports/eep.ttl \
    "$DIR"/../ontology/ontology-no-imports/imports/saref.ttl \
    "$DIR"/../ontology/ontology-no-imports/imports/saref4bldg.ttl \
    "$DIR"/../ontology/ontology-no-imports/imports/saref4ehaw.ttl \
    "$DIR"/../ontology/ontology-no-imports/imports/saref4wear.ttl \
    --no-qvars --pass --nope > "$DIR"/output/ontology.n3

# 2. Extract all triples from this ontology file
eye "$DIR"/output/ontology.n3 "$DIR"/preprocessing/lists.n3 "$DIR"/preprocessing/instantiate-triples.n3 --pass --nope --no-skolem http://eulersharp.sourceforge.net/.well-known/genid/myVariables > "$DIR"/output/triples.n3

# 3. Instantiate all rules from this ontology file
eye "$DIR"/output/triples.n3 --query "$DIR"/preprocessing/instantiate-rules.n3 --nope --no-skolem http://eulersharp.sourceforge.net/.well-known/genid/myVariables > "$DIR"/output/rules.n3

# 4. Create a precompiled Prolog image from the set of triples and rules
eye "$DIR"/output/triples.n3 "$DIR"/output/rules.n3 --image "$DIR"/output/ype.pvm
