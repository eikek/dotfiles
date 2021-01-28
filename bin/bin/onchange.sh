#!/usr/bin/env nix-shell
#! nix-shell -p inotify-tools -i bash

thing=$1
shift

REC_OPT=""
if [ -f "$thing" ]; then
    REC_OPT="-r"
fi

while (true); do
    inotifywait $REC_OPT -e close_write "$thing"
    $@
done
