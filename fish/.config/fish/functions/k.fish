function k
    if string length --quiet $RENKU_ENV
        kubectl -n $RENKU_ENV $argv
    else
        echo "RENKU_ENV is not set!"
        return 1
    end
end
