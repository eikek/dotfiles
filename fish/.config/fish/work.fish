if test (string match "sdsc" (whoami)); or test (string match "ek*" (whoami));
#    set -xg SSH_AUTH_SOCK /Users/ekettner/.gnupg/S.gpg-agent.ssh
    set -gx NIX_SSL_CERT_FILE "/nix/var/nix/profiles/default/etc/ssl/certs/ca-bundle.crt"
    set -gx NIX_PROFILES "/nix/var/nix/profiles/default $HOME/.nix-profile"
    set -gx MANPATH "$HOME/.nix-profile/share/man" $MANPATH
    set -gx PATH ~/bin ~/.nix-profile/bin /nix/var/nix/profiles/default/bin $PATH

#    source ~/.config/fish/direnv.fish
#    dsc generate-completions --shell fish | source
end

set -gx SBT_OPTS "-Xms512M -Xmx2G -Xss32M -Duser.timezone=UTC"

