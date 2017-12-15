#!/usr/bin/env nix-shell
#! nix-shell -i bash -p parallel -p poppler_utils

# Convert a pdf to pdf embedding the fonts
# found here: http://levien.zonnetjes.net/?q=pdf-fontembedding

IN=$1
OUT=$2

if [ "$IN" = "" ]; then
    echo "Input file or folder required"
    exit 1
fi
if [ "$OUT" = "" ]; then
    echo "Output file or folder required"
    exit 1
fi

if [[ -d "$1" ]]; then
    find "$IN" -name "*.pdf" -print0 | parallel -0 $0 {} $OUT/{} \;
else
    if [ -e "$OUT" ]; then
        echo "Output file already exists"
        exit 1
    fi
    mkdir -p $(dirname "$OUT")
    echo "Converting $IN to $OUT …"
    pdftops "$IN"
    gs -q -dNOPAUSE -dBATCH -dPDFSETTINGS=/prepress -sDEVICE=pdfwrite -sOutputFile="$OUT" "${IN%.*}.ps" && rm "${IN%.*}.ps"
fi
