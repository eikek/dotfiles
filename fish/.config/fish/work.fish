if test (string match "Eikes*" (hostname))
    set -xg SSH_AUTH_SOCK /Users/eike/.gnupg/S.gpg-agent.ssh
    set -gx NIX_SSL_CERT_FILE "$HOME/.nix-profile/etc/ssl/certs/ca-bundle.crt"
    set -gx NIX_PATH "$HOME/.nix-defexpr/channels"
    set -gx NIX_PROFILES "/nix/var/nix/profiles/default $HOME/.nix-profile"
    set -gx MANPATH "$HOME/.nix-profile/share/man" $MANPATH
    set -gx PATH ~/bin ~/.nix-profile/bin /opt/homebrew/bin $PATH

    dsc generate-completions --shell fish | source
end

set -gx SBT_OPTS "-Xms512M -Xmx4G -Xss32M -Duser.timezone=UTC"
set -gx AWS_DEFAULT_REGION eu-west-1
set -gx AWS_HOME /home/(whoami)/.aws
set -gx AWS_REGION eu-west-1
set -gx DYNAMO_ENDPOINT "http://localhost:8000"
