# fish init


# from here: https://github.com/fish-shell/fish-shell/issues/602
function reverse_history_search
  history | fzf --no-sort --height 50% --layout reverse | read -l command
  if test $command
    commandline -rb $command
  end
end

function fish_user_key_bindings
  bind \cr reverse_history_search
end

function fish_vterm_prompt_end;
    printf '\e]51;A'(whoami)'@'(hostname)':'(pwd)'\e\\';
end
function track_directories --on-event fish_prompt; fish_vterm_prompt_end; end

set -gx PATH ~/bin $PATH

# set -gx AWS_ACCESS_KEY_ID (gopass tundra/aws/security/eike.kettner/aws-access-key)
# set -gx AWS_SECRET_ACCESS_KEY (gopass tundra/aws/security/eike.kettner/aws-secret-key)
set -gx SBT_OPTS "-Xms512M -Xmx4G -Xss32M -XX:+CMSClassUnloadingEnabled -Duser.timezone=GMT -Dsqlite4java.library.path=services/buy/core/src/test/lib"
set -gx AWS_DEFAULT_REGION eu-west-1
set -gx AWS_HOME /home/(whoami)/.aws
set -gx AWS_REGION eu-west-1

starship init fish | source
