# if binary mangled the terminal
function termfix
    reset
    stty sane
    tput rs1
    clear
    echo -e "\033c"
end
