#!/usr/bin/env bash

TARGET="${1:-$(pwd)}"

meth download --first 10 \
     --target "$TARGET" \
     'siebenstein >16'
