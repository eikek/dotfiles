# fish init

alias l='exa -lha --git'
alias cat='bat'
alias mc='mc --colors="normal=white,black:header=white,red:menunormal=white,color90"'
alias playr="mpv \"\$(find . -type f | perl -MList::Util=shuffle -e 'print shuffle(<STDIN>);' | tail -n 1)\""
alias mux='tmuxinator'
alias mpvu='mpv --audio-device=alsa/iec958:CARD=X5,DEV=0'
alias wallpaper='feh --bg-fill -z ~/.backgrounds/'
