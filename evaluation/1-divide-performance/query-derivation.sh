#!/bin/bash

# construct proof
# -> load intermediate code file resulting from Prolog compilation
# -> run EYE with the compiled ontology, the sensor query, the given context and the specified query (goal)
swipl -x ype.pvm -- "$@" sensor-query.n3 --turtle context.ttl --query find-actions.n3 > outputs/proof.n3

# extract queries from proof
eye outputs/proof.n3 --query queries/query-extraction-goal.n3 --nope --tactic existing-path > outputs/extracted-queries.n3

# substitute variables in queries
eye queries/query-patterns.n3 outputs/extracted-queries.n3 queries/query-substitution-rules.n3 --query queries/query-substitution-goal.n3 --nope > outputs/substituted-queries.n3