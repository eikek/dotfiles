function playr
    set -l file (find . -type f -iname "*.mkv" -or -iname "*.avi" -or -iname "*.mpg" -or -iname "*.mpeg" -or -iname "*.mp4" | perl -MList::Util=shuffle -e 'print shuffle(<STDIN>);' | tail -n 1)
    echo $file
    mpv --volume=0 $file
end
