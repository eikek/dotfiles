#!/usr/bin/env nix-shell
#! nix-shell -p inotify-tools -i bash

thing=$1
shift

while (true); do
    inotifywait -r "$thing" && $@
done
