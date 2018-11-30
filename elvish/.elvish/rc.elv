# elv init file
# https://zzamboni.org/post/my-elvish-configuration-with-commentary/

use epm
epm:install &silent-if-installed=$true   \
  github.com/zzamboni/elvish-modules     \
  github.com/zzamboni/elvish-completions \
  github.com/zzamboni/elvish-themes      \
  github.com/xiaq/edit.elv               \
  github.com/muesli/elvish-libs          \
  github.com/iwoloschin/elvish-packages

use re
use readline-binding

edit:insert:binding[Alt-Backspace] = $edit:kill-small-word-left~
edit:insert:binding[Alt-d] = $edit:kill-rune-right~

use github.com/zzamboni/elvish-modules/alias
alias:new l e:exa -lha --git
alias:new vim e:emacsclient -nw


edit:insert:binding[Tab] = { edit:completion:smart-start; edit:completion:trigger-filter }

use github.com/zzamboni/elvish-completions/vcsh
use github.com/zzamboni/elvish-completions/cd
use github.com/zzamboni/elvish-completions/ssh
use github.com/zzamboni/elvish-completions/builtins

use github.com/xiaq/edit.elv/smart-matcher
smart-matcher:apply

use github.com/zzamboni/elvish-completions/git

# theme

use github.com/zzamboni/elvish-themes/chain
chain:bold-prompt = $true

chain:segment-style = [
  &dir=          session
  &chain=        session
  &arrow=        session
  &git-combined= session
]

edit:prompt-stale-transform = { each [x]{ styled $x[text] "gray" } }


#

E:LESS = "-i -R"
E:EDITOR = "emacsclient -nw"
E:LC_ALL = "de_DE.UTF-8"

-exports- = (alias:export)