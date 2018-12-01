fn findcmp [f a @rest]{
  for n $rest {
    if ($f $n $a) {
      a = $n
    }
  }
  put $a
}

fn min [@list]{
  findcmp [a b]{ < $a $b } $@list
}

fn max [@list]{
  findcmp [a b]{ > $a $b } $@list
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
