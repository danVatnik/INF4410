#!/bin/bash

for i in {1..40}; do
    ARRAY+=(132.207.12.210:8000)
done

time echo ${ARRAY[*]} | xargs -n 1 -P 40 wget -q
