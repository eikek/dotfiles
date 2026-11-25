{
  inputs.nixpkgs.url = "github:NixOS/nixpkgs/nixpkgs-unstable";
  outputs = {nixpkgs, ...}: {
    legacyPackages = nixpkgs.legacyPackages;
  };
}
