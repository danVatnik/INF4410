#!/bin/bash
if [[ -n $1 ]]
then
    for i in {1..40}; do
        ARRAY+=($1)
    done
    time echo ${ARRAY[*]} | xargs -n 1 -P 40 wget -q -O /dev/null
else
    echo "Veuillez spécifier l'adresse IP du serveur."
fi
