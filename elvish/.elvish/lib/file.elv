# Operation on files and filenames

use str
use re
use list

fn exists [file]{
  if ?(test -e $file) { put $true } else { put $false }
}

fn stat [file]{
  fmt = '{"type": "%F",
          "group-id": %g, "group": "%G",
          "user-id": %u, "user": "%U",
          "name": "%n", "size": %s,
          "last-access": "%x", "last-access-sec": %X,
          "last-mod": "%y", "last-mod-sec": %Y
         }'

  e:stat -c $fmt $file | from-json
}

fn size [file]{
  put (stat $file)[size]
}

fn file-type [file]{
  put (stat $file)[type]
}

fn is-directory [file]{
  eq (file-type $file) "directory"
}

fn is-file [file]{
  eq (file-type $file) "regular file"
}

fn is-symlink [file]{
  eq (file-type $file) "symbolic link"
}

fn canonicalize [file]{
  e:readlink -nm $file | slurp
}

fn ext [file]{
  @parts = (splits . $file)
  if (is $parts []) {
    put ""
  } else {
    put $parts[-1]
  }
}

fn basename [file]{
  e = (ext $file)
  n = (- (count $file) (count $e) 1)
  put $file[0:$n]
}

fn content-type [file]{
  put (file -pbL --mime-type $file)
}

fn random-select [&ct=[] &tries=0 @dir]{
  directory = [.]
  if (not-eq [] $dir) {
    directory = $dir
  }
  @collection = (put $@directory | each [d]{ e:find $d -print0 | splits "\000" (slurp) })
  file = 0
  while (and (< $tries 200) (is $file 0)) {
    file = (list:random-select $@collection)
    if (not-eq $ct []) {
      mime = (content-type $file)
      if (re:match $ct $mime) {
        put $file
      } else {
        tries = (+ 1 $tries)
        file = 0
      }
    } else {
      put $file
    }
  }
  if (is $file 0) {
    fail "Nothing found."
  }
}

fn lines [file]{
  cat $file | count
}
