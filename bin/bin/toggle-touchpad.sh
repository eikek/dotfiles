#!/usr/bin/env bash

state=$(synclient -l | grep -i touchpadoff | awk '{print $3}')
nextstate=$(( ($state - 1) * -1 ))

echo "Touchpadoff state is changed from $state to $nextstate"
synclient touchpadoff=$nextstate
