let
  mypkgs = ../workspace/projects/confnix/pkgs/default.nix;
in
{
  packageOverrides =
    if (!builtins.pathExists mypkgs) then p: p
    else import mypkgs;

  allowUnfree = true;
}
