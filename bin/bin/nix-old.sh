#!/usr/bin/env bash

# https://matthewbauer.us/blog/all-the-versions.html

package="$1"
if [ -z "$package" ]; then
    echo "No package given"
    exit 1
fi


cat > /tmp/generate-versions.nix <<EOF
{ channels ? [ "19.09" "19.03" "18.09" "18.03" "17.09" "17.03" #"16.09" "16.03" "15.09" "14.12"
             ]
, attrs ? builtins.attrNames (import <nixpkgs> {})
, system ? builtins.currentSystem
, args ? { inherit system; }
}: let

  getSet = channel:
    (import (builtins.fetchTarball "channel:nixos-\${channel}") args).pkgs;

  getPkg = name: channel: let
    pkgs = getSet channel;
    pkg = pkgs.\${name};
    version = (builtins.parseDrvName pkg.name).version;
  in if builtins.hasAttr name pkgs && pkg ? name then {
    name = version;
    value = pkg;
  } else null;

in builtins.listToAttrs (map (name: {
  inherit name;
  value = builtins.listToAttrs
    (builtins.filter (x: x != null)
      (map (getPkg name) channels));
}) attrs)
EOF

version="$2"
if [ -z "$version" ]; then
    nix eval "(builtins.attrNames (import /tmp/generate-versions.nix {}).$package)"
else
    cmd="$3"
    if [ -z "$cmd" ]; then
        nix-store --add-root $(pwd)/result --indirect -r $(nix-instantiate -E "(import /tmp/generate-versions.nix {}).$package.\"$version\"")
    else
        nix run "(import /tmp/generate-versions.nix {}).$package.\"$version\"" -c $cmd
    fi
fi
