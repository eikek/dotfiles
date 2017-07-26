let
  mypkgs = [ ../../../.confnix/pkgs/default.nix
             ../../../workspace/projects/confnix/pkgs/default.nix
           ];
  pkgs = builtins.filter builtins.pathExists mypkgs;
  emacs =
    let
      dotemacs = builtins.filter builtins.pathExists [
         ../../../.emacs.d/default.nix
         ../../../workspace/projects/dot-emacs/default.nix
      ];
    in
    p: if (dotemacs == []) then {}
       else { myemacs = (import (builtins.head dotemacs) { pkgs = p; }); };

in
{
  packageOverrides =
    if (pkgs == []) then emacs
    else p: (import (builtins.head pkgs) p) // (emacs p);

  allowUnfree = true;
}
