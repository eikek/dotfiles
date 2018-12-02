fn eval [str]{
  tmpf = (mktemp)
  echo $str > $tmpf
  -source $tmpf
  rm -f $tmpf
}


fn y-or-n [&style=default prompt]{
  prompt = $prompt" [y/n] "
  if (not-eq $style default) {
    prompt = (styled $prompt $style)
  }
  print $prompt > /dev/tty
  resp = (head -n1 < /dev/tty)
  eq $resp y
}

fn count-lines [@files]{
  each [f]{ put [$f (cat $f | count)] } $files
}
