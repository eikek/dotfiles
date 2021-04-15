#!/usr/bin/env bash

nix-shell --packages 'rstudioWrapper.override{ packages = with rPackages; [  ggplot2 dplyr xts rmarkdown evaluate digest highr markdown stringr yaml Rcpp htmltools knitr jsonlite base64enc mime ]; }'
