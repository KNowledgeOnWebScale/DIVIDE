#!/bin/bash

DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )

# SCRIPT INITIALIZATION
########################

# check first argument -> should be name of DIVIDE query
if [ "$#" -ne 1 ]; then
    echo "Usage: ./query-derivation.sh <NAME_OF_DIVIDE_QUERY>"
    exit
fi
DIVIDE_QUERY="$1"

# create output directory
mkdir -p "$DIR"/output/"$DIVIDE_QUERY"

# copy context to output files
eye "$DIR"/../evaluations/context.ttl --no-qvars --pass --nope > "$DIR"/output/context.ttl

# mock output of context enrichment step in case no context-enriching queries are being defined
echo " <http://idlab.ugent.be/sensdesc/query#pattern>  <http://idlab.ugent.be/sensdesc#windowParameters> () ." >> "$DIR"/output/context.ttl


# ACTUAL QUERY DERIVATION
##########################

# 1. Semantic reasoning to derive queries
swipl -x output/ype.pvm divide-queries/"$DIVIDE_QUERY"/sensor-query.n3 "$DIR"/output/context.ttl --query divide-queries/"$DIVIDE_QUERY"/goal.n3 > "$DIR"/output/"$DIVIDE_QUERY"/proof.n3

# 2. Query extraction
eye output/"$DIVIDE_QUERY"/proof.n3 "$DIR"/output/context.ttl --query query-derivation/query-extraction-goal.n3 --nope --tactic existing-path > "$DIR"/output/"$DIVIDE_QUERY"/extracted-queries.n3
eye output/"$DIVIDE_QUERY"/proof.n3 "$DIR"/output/context.ttl --query query-derivation/window-parameter-extraction-goal.n3 --nope > "$DIR"/output/"$DIVIDE_QUERY"/extracted-window-parameters.n3

# 3. Input variable substitution
eye divide-queries/"$DIVIDE_QUERY"/query-pattern.n3 "$DIR"/output/"$DIVIDE_QUERY"/extracted-queries.n3 "$DIR"/output/"$DIVIDE_QUERY"/extracted-window-parameters.n3 query-derivation/query-input-variable-substitution-rules.n3 query-derivation/query-input-variable-substitution-supported-datatypes.n3 --query query-derivation/query-input-variable-substitution-goal.n3 --nope > "$DIR"/output/"$DIVIDE_QUERY"/queries-after-input-variable-substitution.n3

# 4. Window parameter substitution
eye "$DIR"/output/"$DIVIDE_QUERY"/queries-after-input-variable-substitution.n3 query-derivation/query-dynamic-window-parameter-substitution-rules.n3 query-derivation/trigger/trigger-context-change.n3 --query query-derivation/query-dynamic-window-parameter-substitution-goal.n3 --nope > "$DIR"/output/"$DIVIDE_QUERY"/queries-after-dynamic-window-parameter-substitution.n3
eye "$DIR"/output/"$DIVIDE_QUERY"/queries-after-dynamic-window-parameter-substitution.n3 query-derivation/query-static-window-parameter-substitution-rules.n3 --query query-derivation/query-static-window-parameter-substitution-goal.n3 --nope > "$DIR"/output/"$DIVIDE_QUERY"/substituted-queries.n3
