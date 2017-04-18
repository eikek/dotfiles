#!/usr/bin/env bash

TARGET="${1:-$(pwd)}"

meth download --first 10 \
     --tvdb-firstaired --tvdb-seriesid 83214 \
     --target "$TARGET" \
     --pattern '%[year]/s%[year]e%[tvdb.airedEpisodeNumber]_%[tvdb.episodeName]' \
     'tatort station:ard >80 -audiodeskription -"(ad)" -hörfassung (| dow:mon dow:sun)'
