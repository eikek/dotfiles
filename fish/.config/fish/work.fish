set -gx NIX_PATH "$HOME/.nix-defexpr/channels"
set -gx NIX_PROFILES "/nix/var/nix/profiles/default $HOME/.nix-profile"
set -gx NIX_SSL_CERT_FILE "$HOME/.nix-profile/etc/ssl/certs/ca-bundle.crt"
set -gx MANPATH "$HOME/.nix-profile/share/man" $MANPATH
set -xg SSH_AUTH_SOCK /Users/eike/.gnupg/S.gpg-agent.ssh

set -gx PATH ~/bin ~/.nix-profile/bin $PATH

dsc generate-completions --shell fish | source

# set -gx AWS_ACCESS_KEY_ID (gopass tundra/aws/security/eike.kettner/aws-access-key)
# set -gx AWS_SECRET_ACCESS_KEY (gopass tundra/aws/security/eike.kettner/aws-secret-key)
set -gx SBT_OPTS "-Xms512M -Xmx4G -Xss32M -Duser.timezone=GMT"
set -gx AWS_DEFAULT_REGION eu-west-1
set -gx AWS_HOME /home/(whoami)/.aws
set -gx AWS_REGION eu-west-1
