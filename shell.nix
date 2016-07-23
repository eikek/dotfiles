{ pkgs ? import <nixpkgs> {} }:

with pkgs;

stdenv.mkDerivation {
  name = "env";

  buildInputs = [ stow ];

  shellHook = ''
    install_all() {
      find .  -maxdepth 1 -type d -name "[a-z]*" -print | while read f;
      do
        echo "Install $(basename $f) ..."
        stow -t $HOME -R $(basename $f)
      done
    }
    export -f install_all;
  '';
}
