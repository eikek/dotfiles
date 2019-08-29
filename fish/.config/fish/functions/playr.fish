function playr
    mpv (find . -type f | perl -MList::Util=shuffle -e 'print shuffle(<STDIN>);' | tail -n 1)
end