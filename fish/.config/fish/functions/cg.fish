function cg
    set dir (git rev-parse --show-toplevel)
    echo "$dir"
    cd "$dir"
end
