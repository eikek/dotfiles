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
