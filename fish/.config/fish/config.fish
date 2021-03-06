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

starship init fish | source
