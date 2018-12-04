fn is-list [v]{
  put (==s (kind-of $v) "list")
}

fn is-map [v]{
  put (==s (kind-of $v) "map")
}


# Example:
#  put **.org | each $file:stat~ | take 5 | each (list:get name)
fn get [n &mapchar=.]{
  use str
  put [list-or-map]{
    if (str:contains $n $mapchar) {
      @parts = (splits $mapchar $n)
      cur = $list-or-map
      while (> (count $parts) 0) {
        head @parts = $@parts
        cur = $cur[$head]
      }
      put $cur
    } else {
      put $list-or-map[$n]
    }
  }
}

1 = (get 0)
2 = (get 1)
3 = (get 2)
4 = (get 3)

fn findcmp [f a @rest]{
  for n $rest {
    if ($f $n $a) {
      a = $n
    }
  }
  put $a
}

# Example:
#   put **.org | peach (list:with $file:lines~) | list:min &by-i=1 (all)
#
fn min [@list &by=[x]{ put $x } &by-i=-1]{
  if (not-eq $by-i -1) {
    by = [x]{ put ((get $by-i) $x) }
  }
  findcmp [a b]{ < ($by $a) ($by $b) } $@list
}

fn max [@list &by=[x]{ put $x } &by-i=-1]{
  if (not-eq $by-i -1) {
    by = [x]{ put ((get $by-i) $x) }
  }
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

fn -zip [l1 @l2]{
  c = (min (count $l1) (count $l2))
  range $c | peach [i]{
    put [$l1[$i] $l2[$i]]
  }
}

# Example:
#    put **.org | each (list:filter [x]{ < (count $x) 18 })
#
fn filter [f]{
  put [e]{
    if ($f $e) { put $e }
  }
}

fn filterNot [f]{
  put [e]{
    if (not ($f $e)) { put $e }
  }
}

fn -cmp [a b]{
  try {
    if (< $a $b) { put -1 } elif (> $a $b) { put 1 } else { put 0 }
  } except _ {
    if (<s $a $b) { put -1 } elif (>s $a $b) { put 1 } else { put 0 }
  }
}

# Example:
#   put **.elv | peach (list:with $file:lines~) | list:sort &by-i=1 (all)
#
fn sort [@things &by=[x]{ put $x } &by-i=-1]{
  if (not-eq $by-i -1) {
    by = [x]{ put ((get $by-i) $x) }
  }
  if (not-eq $things []) {
    h @tail = $@things
    hv = ($by $h)
    sort &by=$by (each [e]{ if (< (-cmp ($by $e) $hv) 0) { put $e } } $tail)
    each [e]{ if (== (-cmp ($by $e) $hv) 0) { put $e } } $tail
    put $h
    sort &by=$by (each [e]{ if (> (-cmp ($by $e) $hv) 0) { put $e } } $tail)
  }
}

fn sum [@things &by=[x]{ put $x } &by-i=-1]{
  if (not-eq $by-i -1) {
    by = [x]{ put ((get $by-i) $x) }
  }

  res = 0
  each [e]{ res = (+ $res ($by $e)) } $things
  put $res
}

fn avg [@things &by=[x]{ put $x} &by-i=-1]{
  if (not-eq $by-i -1) {
    by = [x]{ put ((get $by-i) $x) }
  }
  s = (sum &by=$by $@things)
  put (/ $s (count $things))
}

fn median [@things &by=[x]{ put $x } &by-i=-1]{
  use str

  if (not-eq $by-i -1) {
    by = [x]{ put ((get $by-i) $x) }
  }
  @sorted = (sort &by=$by $@things)
  len = (count $things)
  mid = (/ $len 2)
  if (str:contains $mid .) {
    mid = (+ $mid 0.5)
  }
  put $sorted[$mid]
}

fn reverse [@things]{
  len = (count $things)
  range $len | each [i]{
    put $things[(- $len $i 1)]
  }
}

# Example:
#   put **.org | take 10 | each (list:with $file:lines~)
#
fn with [f]{
  put [e]{
    v = ($f $e)
    if (and (is-list $v) (is-list $e)) {
      put [$@e $@v]
    } elif (is-list $v) {
      put [$e $@v]
    } elif (is-list $e) {
      put [$@e $v]
    } else {
      put [$e ($f $e)]
    }
  }
}

fn withm [f &k=key &v=value]{
  put [e]{
    val = ($f $e)
    if (is-map $val) {
      assoc $val $k $e
    } else {
      assoc (assoc [&] $k $e) $v $val
    }
  }
}
