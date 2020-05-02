let
  mypkgs = [ ../../../.confnix/pkgs/default.nix
             ../../../workspace/projects/confnix/pkgs/default.nix
           ];
  pkgs = builtins.filter builtins.pathExists mypkgs;
in
{
  packageOverrides =
    p: (import (builtins.head pkgs) p);

  allowUnfree = true;
}
