function playr
    set -l file (find . -type f | perl -MList::Util=shuffle -e 'print shuffle(<STDIN>);' | tail -n 1)
    echo $file
    mpv $file
end
