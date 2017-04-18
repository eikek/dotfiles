#!/usr/bin/env bash

TARGET="${1:-$(pwd)}"

meth download --first 10 \
     --target "$TARGET" \
     'station:ard >9 title:sandmännchen -gebärdensprache'
