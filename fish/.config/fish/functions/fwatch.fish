# Use inotifywait in an endless loop for executing something on file change
#
# fwatch [file to watch] [command]
function fwatch
    set file $argv[1]
    set cmd $argv
    set -e cmd[1]
    echo "file: $file"
    echo "cmd: $cmd"
    while true;
        inotifywait -e close_write "$file"
        echo ">> Running $cmd"
        $cmd
    end
end
