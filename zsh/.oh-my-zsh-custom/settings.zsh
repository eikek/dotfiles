#!/bin/zsh
setopt COMBINING_CHARS
export TERM=xterm-256color

# as proposed at http://fruit.je/utf-8
export LANG=de_DE.UTF-8
export LESSCHARSET=utf-8
export PERL_UTF8_LOCALE=1 PERL_UNICODE=AS
export SBT_OPTS="-Xmx1024m"

GPG_TTY=$(tty)
export GPG_TTY
unset SSH_AGENT_PID
export SSH_AUTH_SOCK="${XDG_RUNTIME_DIR:-/run/user/$(id -u)}/gnupg/S.gpg-agent.ssh"

# Base16 Shell
if [ -d $ZSH_CUSTOM/base16-shell ]; then
    BASE16_SHELL="$ZSH_CUSTOM/base16-shell/scripts/base16-twilight.sh"
    [[ -s $BASE16_SHELL ]] && source $BASE16_SHELL
fi

restart() {
    tmuxinator stop $1 && sleep 1 && tmuxinator start $1
}

# alias
alias l='exa -la --git'
alias vim='emacsclient -nw'
alias vi='emacsclient -nw'
alias ec='emacsclient -c'
alias e='emacsclient'
alias mc='mc --colors="normal=white,black:header=white,red:menunormal=white,color90"'
alias mvni='mvn install -DskipTests=true -T1C'
#alias nixq="nix-env -qaP | grep -i"
alias playr="mpv \"\$(find . -type f | perl -MList::Util=shuffle -e 'print shuffle(<STDIN>);' | tail -n 1)\""
alias killsbt="ps aux|grep sbt |grep -v grep| awk '{print \$2}' |xargs kill -9"
alias tess='tesseract stdin stdout -l deu -psm 1 --tessdata-dir /run/current-system/sw/share/tessdata < '
alias mux='tmuxinator'
alias pill-nas='PILL_OPTS="-Dpill.cli.endpoint=http://nas:10549" pill'
alias mpvu='mpv --audio-device=alsa/iec958:CARD=X5,DEV=0'

rwhich() { readlink -e $(which $1) }

if [ grep -i nixos /etc/os-release > /dev/null 2> /dev/null ]; then
    NIXENV_SH=/home/$USER/.nix-profile/etc/profile.d/nix.sh
    if [ -r $NIXENV_SH ]; then
        . $NIXENV_SH
    fi
fi

if [[ $OSTYPE == darwin* ]];
then
    alias brew='SSL_CERT_FILE= brew'
else
    # tweak LS_COLORS to avoid backgrounds where I don't like it
    # dircolors not available on darwin
    eval $(dircolors | sed 's/ow=\([0-9]*\);[0-9]*/ow=\1;40/')
fi

if which direnv 2>&1 > /dev/null; then
    eval "$(direnv hook zsh)"
else
    echo "Program 'direnv' not installed."
fi

find $ZSH_CUSTOM -name "*local.zsh" | while read f; do
    source $f
done
