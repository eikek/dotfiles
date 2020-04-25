#!/usr/bin/env bash
#
# Find or start a program.
#
# First argument is the window string to search for, second argument
# the program to start if jumping fails.

SEARCH="$1"
CMD="$2"
declare -i JUMPED=0
while read id; do
    herbstclient jumpto $id 2>&1 >/dev/null
    if [ $? -eq 0 ]; then
        JUMPED=1
        break
    fi
done < <(xwininfo -tree -root | grep -i "\"$SEARCH\")" | awk '{print $1}')

if [ $JUMPED == 0 ]; then
    $CMD &
fi
