#!/usr/bin/env bash

dry=0
if [ "$1" = "--dry" ]; then
    dry=1
    shift
fi

GOOD='a-zA-z0-9 _\-.,:öäüÖÄÜ'
for f in "$@"; do
    ext="${f##*.}"
    name=$(mediainfo --Inform="General;%Track/Position%-%Composer%-%Title%.$ext" "$f" | \
               tr 'è' 'e' | \
               tr 'È' 'E' | \
               tr 'à' 'a' | \
               tr 'À' 'A' | \
               tr 'ß' "ss" | \
               tr 'ü' "ue" | \
               tr 'Ü' "Ue" | \
               tr 'ä' "ae" | \
               tr 'Ä' "Ae" | \
               tr 'ö' "oe" | \
               tr 'Ö' "Oe" | \
               tr -d -c "$GOOD" | tr ':' ',')
    if [ $dry -eq 1 ]; then
        echo "$f --> $name"
    else
        if [ -e "$name" ]; then
            echo "Already correct name: $name"
        else
            echo "Move $f --> $name"
            mv "$f" "$name"
        fi
    fi
done
