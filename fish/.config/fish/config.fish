# fish init

alias l='exa -lha --git'
alias cat='bat'
alias mc='mc --colors="normal=white,black:header=white,red:menunormal=white,color90"'
alias playr="mpv (find . -type f | perl -MList::Util=shuffle -e 'print shuffle(<STDIN>);' | tail -n 1)"
alias mux='tmuxinator'
alias mpvu='mpv --audio-device=alsa/iec958:CARD=X5,DEV=0'
alias wallpaper='feh --bg-fill -z ~/.backgrounds/'


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
