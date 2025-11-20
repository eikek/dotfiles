function qr
    nix run nixpkgs#qrencode -- -m5 -t ansiutf8 "$argv"
end
