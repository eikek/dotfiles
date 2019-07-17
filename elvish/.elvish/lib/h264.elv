# - ffmpeg, encode to mkv
# - include all deu/ger/eng audio, copy
# - encode all video via h264 and crf=25

use str

fn -basename [f]{
  li = (str:last-index $f ".")
  put $f[0:$li]
}

fn -stream-bytes [f stream]{
  si = $stream[0]':'$stream[1]': '$stream[2]
  @bytes = (ffprobe $f 2>&1 | grep -A10 $si | grep NUMBER_OF_BYTES | head -n1 | splits ":" (all))
  put (str:trim-space $bytes[1])
}

fn -read-stream [f]{
  put [line]{
    @parts = (splits ':' $line | each $str:trim-space~)
    bytes = (-stream-bytes $f $parts)
    put [$@parts $bytes]
  }
}

fn -stream-type [stream]{
  put $stream[2]
}

fn -stream-index [stream]{
  l = $stream[1]
  if (> (count $l) 5) {
    put $l[:-5]
  } else {
    put $l
  }
}

fn -stream-lang [stream]{
  l = $stream[1]
  put (str:to-lower $l[2:-1])
}

fn props [f]{
  streams = [(ffprobe $f 2>&1 | grep "Stream #" | each (-read-stream $f))]
  # take first ger/deu and first eng audio stream and first deu/ger subtitle
  videos = []
  audios = [&]
  subtitles = [&]

  each [stream]{
    st = (-stream-type $stream)
    if (eq $st "Video") {
      videos = [$stream $@videos]
    } elif (eq $st "Audio") {
      lng = (-stream-lang $stream)
      if (or (eq $lng "deu") (eq $lng "ger")) {
        if (has-key $audios deu) {
          cas = $audios[deu]
          if (> $stream[-1] $cas[-1]) {
            audios = (assoc $audios deu $stream)
          }
        } else {
          audios = (assoc $audios deu $stream)
        }
      } elif (and (not (has-key $audios "eng")) (eq $lng eng)) {
        audios = (assoc $audios eng $stream)
      }
    } elif (eq $st "Subtitle") {
      lng = (-stream-lang $stream)
      if (and (not (has-key $subtitles "deu")) (or (eq $lng "deu") (eq $lng "ger"))) {
        subtitles = (assoc $subtitles deu $stream)
      } elif (and (not (has-key $subtitles "eng")) (eq $lng eng)) {
        subtitles = (assoc $subtitles eng $stream)
      }
    }
  } $streams

  put $videos $audios $subtitles
}

fn -make-target [f]{
  put (-basename $f).transcoded.mkv
}

fn encode [f &crf=23 &vidopts=["-tune" "film"] &dry=$false]{
  vids audio subs = (props $f)
  cmd = [-i $f]

  each [vs]{
    cmd = [$@cmd -map 0:(-stream-index $vs)]
  } $vids

  cmd = [$@cmd "-c:v" libx264 "-max_muxing_queue_size" "9999" "-crf" $crf $@vidopts]

  if (has-key $audio deu) {
    stream = $audio[deu]
    cmd = [$@cmd -map 0:(-stream-index $stream)]
  }
  if (has-key $audio eng) {
    stream = $audio[eng]
    cmd = [$@cmd -map 0:(-stream-index $stream)]
  }
  cmd = [$@cmd "-c:a" copy]

  if (and (has-key $subs deu) (not-eq (keys $audio) deu)) {
    stream = $subs[deu]
    cmd = [$@cmd -map 0:(-stream-index $stream)]
  }
  if (and (has-key $subs eng) (not-eq (keys $audio) eng)) {
    stream = $subs[eng]
    cmd = [$@cmd -map 0:(-stream-index $stream)]
  }
  cmd = [$@cmd "-c:s" copy (-make-target $f)]

  print "Running: ffmpeg "
  echo $cmd
  if (not $dry) {
    ffmpeg $@cmd
  }
}

fn to-encoded [&crf=23 &vidopts=["-tune" "film"] &dry=$false &overwrite=$false]{
  put [file]{
    if (str:contains $file transcoded) {
      echo "File "$file" already transcoded"
    } else {
      target = (-make-target $file)
      if ?(test -e $target) {
        if $overwrite {
          rm -f $target
          encode $file &crf=$crf &vidopts=$vidopts &dry=$dry
        } else {
          echo "Not overwriting target file: "$target
        }
      } else {
        encode $file &crf=$crf &vidopts=$vidopts &dry=$dry
      }
    }
  }
}
