#!/usr/bin/env bash

dry=0
if [ "$1" = "--dry" ]; then
    dry=1
    shift
fi

for f in "$@"; do
    name=$(mediainfo --Inform="General;%Track/Position%-%Title%.mp3" "$f")
    if [ $dry -eq 1 ]; then
        echo "$f --> $name"
    else
        mv "$f" "$name"
    fi
done
