let
  mypkgs = [ ../.confnix/pkgs/default.nix ../workspace/projects/confnix/pkgs/default.nix ];
  pkgs = builtins.filter builtins.pathExists mypkgs;
in
{
  packageOverrides =
    if (pkgs == []) then p: p
    else import (builtins.head pkgs);

  allowUnfree = true;
}
