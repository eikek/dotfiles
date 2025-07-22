function rmrec
    set -l ftype $argv[1]
    if [ "$ftype" = "-d" ]
        set ftype "d"
    else
        set ftype "f"
    end
    if [ -z "$argv[2]" ]
        echo "No file or directory name given."
        return 1
    end
    set -l dry 1
    if [ "--dry" = "$argv[3]" ]
        set dry 1
        echo "Dry run!"
    else if [ "" = "$argv[3]" ]
        set dry 0
    else
        echo "Invalid argument: $argv[3]"
        return 1
    end
    for f in (find . -type $ftype -name "$argv[2]")
        if [ $dry = 0 ]
            set_color red
            echo -n "$f…"
            rm -rf "$f"
            echo " ❌"
            set_color normal
        else
            set_color red
            echo "❌ $f…"
            set_color normal
        end
    end
end
