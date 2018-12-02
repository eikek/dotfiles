fn findcmp [f a @rest]{
  for n $rest {
    if ($f $n $a) {
      a = $n
    }
  }
  put $a
}

fn min [@list &by=[x]{ put $x }]{
  findcmp [a b]{ < ($by $a) ($by $b) } $@list
}

fn max [@list &by=[x]{ put $x }]{
  findcmp [a b]{ > ($by $a) ($by $b) } $@list
}

fn random-select [&n=1 &from=0 &to=-1 @list]{
  c = $to
  if (< $c 1) {
    c = (count $list)
  }
  range $n | peach [_]{
    put $list[(randint $from $c)]
  }
}

fn zip [l1 @l2]{
  c = (min (count $l1) (count $l2))
  range $c | peach [i]{
    put [$l1[$i] $l2[$i]]
  }
}

fn filter [f @list]{
  each [e]{ if ($f $e) { put $e } } $list
}

fn filterNot [f @list]{
  each [e]{ if (not ($f $e)) { put $e } } $list
}

fn get-n [n @list]{
  each [e]{ put $e[$n] } $list
}

fn first [@list]{
  get-n 0 $@list
}

fn second [@list]{
  get-n 1 $@list
}

fn third [@list]{
  get-n 2 $@list
}

# Example:
#   list:sort [a 1] [b 5] [c 3] [d 2] [e 4] &by=$list:second~
#
fn sort [@things &by=[x]{ put $x }]{
  if (not-eq $things []) {
    h @tail = $@things
    hv = ($by $h)
    sort &by=$by (each [e]{ if (< ($by $e) $hv) { put $e } } $tail)
    each [e]{ if (== ($by $e) $hv) { put $e } } $tail
    put $h
    sort &by=$by (each [e]{ if (> ($by $e) $hv) { put $e } } $tail)
  }
}

fn sum [@things &by=[x]{ put $x }]{
  res = 0
  each [e]{ res = (+ $res ($by $e)) } $things
  put $res
}

fn reverse [@things]{
  len = (count $things)
  range $len | each [i]{
    put $things[(- $len $i 1)]
  }
}
