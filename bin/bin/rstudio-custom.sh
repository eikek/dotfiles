#!/usr/bin/env bash

nix-shell --packages 'rstudioWrapper.override{ packages = with rPackages; [ tidyr ggplot2 dplyr xts rmarkdown evaluate digest highr markdown stringr yaml Rcpp htmltools knitr jsonlite base64enc mime lmtest tidyverse reshape2 broom ]; }' --run rstudio
