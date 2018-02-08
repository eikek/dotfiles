#!/usr/bin/env nix-shell
#! nix-shell -p inotify-tools -i bash

while (true); do
    inotifywait -r . && $@
done
