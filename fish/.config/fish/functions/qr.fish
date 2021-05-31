function qr
    nix-shell -p qrencode --run "qrencode -m10 -o - '$argv' | feh -Z -"
end
