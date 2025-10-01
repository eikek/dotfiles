function klog
    if string length --quiet $RENKU_ENV
        kubectl -n $RENKU_ENV logs -f deployment/$RENKU_ENV-$argv
    else
        echo "RENKU_ENV is not set!"
        return 1
    end
end
