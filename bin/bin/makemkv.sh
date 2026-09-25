#!/usr/bin/env bash
# https://forum.makemkv.com/forum/viewtopic.php?f=5&t=1053
sudo modprobe sg
export NIXPKGS_ALLOW_UNFREE=1
nix run --impure nixpkgs#makemkv
