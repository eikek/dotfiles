#!/usr/bin/env bash

TARGET="${1:-$(pwd)}"

meth download --first 10 \
     --target "$TARGET" \
     'station:ard >5 title:sandmännchen -gebärdensprache'
