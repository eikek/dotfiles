#!/usr/bin/env nix-shell
#!nix-shell -p xxd -i bash

set -e

# add cover art to music files

addViaFFmpeg() {
    cover="$1"
    srcfile="$2"
    outfile="$3"

    ffmpeg -i "$srcfile" -i "$cover" -loglevel quiet -y -acodec copy -map_metadata 0 -map 0 -map 1 "$outfile"
    mv "$outfile" "$srcfile"
}

addMp3() {
    outfile="$(mktemp mp3cover-XXXXXXX.mp3)"
    chmod 644 "$outfile"
    addViaFFmpeg "$1" "$2" "$outfile"
}

addOgg() {
    # this works, but you cannot seek in the resulting ogg
    # outfile="$(mktemp oggcover.XXXXXXX).ogg"
    # addViaFFmpeg "$1" "$2" "$outfile"

    # now the complex solution…
    # copied from https://github.com/biapy/howto.biapy.com/blob/master/various/mussync-tools
    OUTPUT_FILE="$2"
    IMAGE_PATH="$1"
    IMAGE_MIME_TYPE="$(file -b --mime "$1" | cut -d';' -f1)"
    # Export existing comments to file.
    local COMMENTS_PATH="$(command mktemp -t "tmp.XXXXXXXXXX")"
    command vorbiscomment --list --raw "${OUTPUT_FILE}" > "${COMMENTS_PATH}"

    # Remove existing images.
    command sed -i -e '/^metadata_block_picture/d' "${COMMENTS_PATH}"

    # Insert cover image from file.

    # metadata_block_picture format.
    # See: https://xiph.org/flac/format.html#metadata_block_picture

    local IMAGE_WITH_HEADER="$(command mktemp -t "tmp.XXXXXXXXXX")"
    local DESCRIPTION=""

    # Reset cache file.
    echo -n "" > "${IMAGE_WITH_HEADER}"

    # Picture type <32>.
    command printf "0: %.8x" 3 | command xxd -r -g0 \
                                         >> "${IMAGE_WITH_HEADER}"
    # Mime type length <32>.
    command printf "0: %.8x" $(echo -n "${IMAGE_MIME_TYPE}" | command wc -c) \
        | command xxd -r -g0 \
                  >> "${IMAGE_WITH_HEADER}"
    # Mime type (n * 8)
    echo -n "${IMAGE_MIME_TYPE}" >> "${IMAGE_WITH_HEADER}"
    # Description length <32>.
    command printf "0: %.8x" $(echo -n "${DESCRIPTION}" | command wc -c) \
        | command xxd -r -g0 \
                  >> "${IMAGE_WITH_HEADER}"
    # Description (n * 8)
    echo -n "${DESCRIPTION}" >> "${IMAGE_WITH_HEADER}"
    # Picture with <32>.
    command printf "0: %.8x" 0 | command xxd -r -g0 \
                                         >> "${IMAGE_WITH_HEADER}"
    # Picture height <32>.
    command printf "0: %.8x" 0 | command xxd -r -g0 \
                                         >> "${IMAGE_WITH_HEADER}"
    # Picture color depth <32>.
    command printf "0: %.8x" 0 | command xxd -r -g0 \
                                         >> "${IMAGE_WITH_HEADER}"
    # Picture color count <32>.
    command printf "0: %.8x" 0 | command xxd -r -g0 \
                                         >> "${IMAGE_WITH_HEADER}"
    # Image file size <32>.
    command printf "0: %.8x" $(command wc -c "${IMAGE_PATH}" \
                                   | command cut --delimiter=' ' --fields=1) \
        | command xxd -r -g0 \
                  >> "${IMAGE_WITH_HEADER}"
    # Image file.
    command cat "${IMAGE_PATH}" >> "${IMAGE_WITH_HEADER}"

    echo "metadata_block_picture=$(command base64 --wrap=0 < "${IMAGE_WITH_HEADER}")" >> "${COMMENTS_PATH}"

    # Update vorbis file comments.
    command vorbiscomment --write --raw --commentfile "${COMMENTS_PATH}" "${OUTPUT_FILE}"

    # Delete cache file.
    command rm "${IMAGE_WITH_HEADER}"
    # Delete comments file.
    command rm "${COMMENTS_PATH}"
}

addFlac() {
    cover="$1"
    flacfile="$2"
    metaflac --import-picture-from="$cover" "$flacfile"
}

fileType() {
    mtype=$(file -b --mime "$1" | cut -d';' -f1)
    case "$mtype" in
        audio/mpeg)
            echo "mp3"
            ;;

        audio/ogg)
            echo "ogg"
            ;;

        audio/flac)
            echo "flac"
            ;;

        *)
            echo "unknown"
            ;;
    esac
}


coverfile="$1"
if [ -z "$coverfile" ]; then
    echo "No coverfile given."
    exit 1
fi
shift

if [ -z "$1" ]; then
    echo "No audio files given"
    exit 1
fi

for f in "$@"; do
    ftype=$(fileType "$f")
    echo -n "Adding cover $coverfile to $ftype file $f ..."
    case "$ftype" in
        mp3)
            addMp3 "$coverfile" "$f"
            ;;
        ogg)
            addOgg "$coverfile" "$f"
            ;;

        flac)
            addFlac "$coverfile" "$f"
            ;;

        *)
            echo "Unknown file type: $f"
            ;;
    esac
    echo "ok"
done
