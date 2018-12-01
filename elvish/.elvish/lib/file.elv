# Operation on files and filenames

use str
use re
use list

fn exists [@files]{
  each [f]{
    if ?(test -e $f) { put $true } else { put $false }
  } $files
}

fn stat [@files]{
  fmt = '{"type": "%F",
          "group-id": %g, "group": "%G",
          "user-id": %u, "user": "%U",
          "name": "%n", "size": %s,
          "last-access": "%x", "last-access-sec": %X,
          "last-mod": "%y", "last-mod-sec": %Y
         }'

  each [f]{ e:stat -c $fmt $f | from-json } $files
}

fn size [@files]{
  stat $@files | each [i]{ put $i[size] }
}

fn file-type [@files]{
  stat $@files | each [i]{ str:to-lower $i[type] }
}

fn is-directory [@files]{
  file-type $@files | each [ft]{ eq $ft "directory" }
}

fn is-file [@files]{
  file-type $@files | each [ft]{ eq $ft "regular file" }
}

fn is-symlink [@files]{
  file-type $@files | each [ft]{ eq $ft "symbolic link" }
}

fn canonicalize [@files]{
  each [f]{ e:readlink -nm $f | slurp } $files
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

fn content-type [@files]{
  each [f]{ put (file -pbL --mime-type $f) } $files
}

fn random-select [&content-type=[] &tries=0 @dir]{
  directory = [.]
  if (not-eq [] $dir) {
    directory = $dir
  }
  @collection = (put $@directory | each [d]{ e:find $d -print0 | splits "\000" (slurp) })
  file = 0
  while (and (< $tries 200) (is $file 0)) {
    file = (list:random-select $@collection)
    if (not-eq $content-type []) {
      mime = (content-type $file)
      if (re:match $content-type $mime) {
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
