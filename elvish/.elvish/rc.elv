# elv init file
# https://zzamboni.org/post/my-elvish-configuration-with-commentary/

use epm
epm:install &silent-if-installed=$true   \
  # github.com/zzamboni/elvish-modules     \
  # github.com/zzamboni/elvish-completions \
  # github.com/zzamboni/elvish-themes      \
  # github.com/xiaq/edit.elv               \
  # github.com/muesli/elvish-libs          \
  # github.com/iwoloschin/elvish-packages \
  github.com/eikek/elv-modules

use re
use readline-binding
use str
use github.com/eikek/elv-modules/file
use github.com/eikek/elv-modules/list
use github.com/eikek/elv-modules/csv
use github.com/eikek/elv-modules/nix
use github.com/eikek/elv-modules/completer
use github.com/eikek/elv-modules/pdf2txt

edit:insert:binding[Alt-Backspace] = $edit:kill-small-word-left~

# Strange is that there is no $edit:kill-small-word-right~ variable
# set. So I simulate it….
fn edit-kill-small-word-right {
  $edit:move-dot-right-word~
  $edit:kill-small-word-left~
}

edit:insert:binding[Alt-d] = $edit-kill-small-word-right~

# Aliases

fn playr [@dir]{
  e:mpv (file:random-select &ct='video/.*' $@dir)
}

fn cheat [@opts]{
  if (eq $opts []) {
    curl cht.sh
  } else {
    curl cht.sh/$@opts
  }
}

use github.com/zzamboni/elvish-modules/alias
alias:new l e:exa -la --git
alias:new cat e:bat --theme DarkNeon --color auto --decorations auto --style changes,numbers,grid
alias:new cp e:rsync -avP
alias:new vim e:emacsclient -nw
alias:new amm e:bash -c amm
alias:new gpgc e:gpg-connect-agent updatestartuptty /bye
alias:new ec e:emacsclient --create-frame
alias:new mc e:mc --colors="normal=white,black:header=white,red:menunormal=white,color90"
alias:new mux e:tmuxinator
alias:new pill-nas E:PILL_OPTS=-Dpill.cli.endpoint=http://nas:10549 pill
alias:new weather curl wttr.in

# Completion

edit:insert:binding[Tab] = { edit:completion:smart-start; edit:completion:trigger-filter }

use github.com/zzamboni/elvish-completions/vcsh
use github.com/zzamboni/elvish-completions/cd
use github.com/zzamboni/elvish-completions/ssh
use github.com/zzamboni/elvish-completions/builtins

use github.com/xiaq/edit.elv/smart-matcher
smart-matcher:apply

use github.com/zzamboni/elvish-completions/git

# Theme

use github.com/zzamboni/elvish-themes/chain
chain:bold-prompt = $true

chain:segment-style = [
  &dir=          session
  &chain=        session
  &arrow=        session
  &git-combined= session
]

edit:prompt-stale-transform = { each [x]{ styled $x[text] "gray" } }


# Environment

E:LESS = "-i -R"
E:EDITOR = "emacsclient -nw"
E:LC_ALL = "de_DE.UTF-8"

-exports- = (alias:export)
