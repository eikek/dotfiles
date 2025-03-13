#!/usr/bin/env amm
import $ivy.`co.fs2::fs2-core:3.2.4`
import $ivy.`co.fs2::fs2-io:3.2.4`
import $ivy.`com.monovore::decline:2.1.0`
import $ivy.`com.monovore::decline-effect:2.1.0`
import $ivy.`org.typelevel::cats-parse:0.3.6`

// Combines ffmpeg commands to transcode a video in the following way:
// - include german and english
// - include subtitles
// - audio copy as is
// - video encode using h265

import cats.effect._
import cats.implicits._
import cats.data.{NonEmptyList => Nel}
import fs2.Stream
import fs2.io.file._
import com.monovore.decline._
import com.monovore.decline.effect._
import java.nio.file.{Path => JPath}
import java.io.StringWriter
import scala.collection.mutable.ListBuffer

object Main extends CommandIOApp(
  name = "h264",
  header = "FFMPeg wrapper to encode videos",
  version = "0.0.1"
){

  def main: Opts[IO[ExitCode]] =
    Options.default.map { cfg =>
      ffprobe(cfg, cfg.files.head)
        .map(v => Parsing.parse(v.mkString("\n")))
        .map(println).as(ExitCode.Success)
    }

  def makeTarget(file: Path): Path =
    file.fileName

  def exec(cmd: Seq[String]): IO[Vector[String]] = IO {
    import sys.process._
    val buffer = ListBuffer.empty[String]
    def write: String => Unit = s => buffer.append(s)
    val res = Process(cmd).run(ProcessLogger(write, write))
    if (res.exitValue != 0) sys.error(s"$cmd not successfull: ${buffer.toString}")
    else buffer.toVector
  }


  def ffprobe(cfg: Options, file: Path): IO[Vector[String]] =
    exec(Seq(cfg.ffprobe, file.normalize.absolute.toString))
      .map(_.dropWhile(s => !s.startsWith("Input ")))

}

object model {
  sealed trait Track
  final case class Audio(num: Int, codec: String, lang: String, size: Long) extends Track
  final case class Video(num: Int, codec: String, lang: String, size: Long) extends Track
  final case class Subtitle(num: Int, name: String, lang: String, size: Long) extends Track

  final case class Container(title: String, tracks: Vector[Track])
}

final case class Options(
  crf: Int,
  ffmpeg: String,
  ffprobe: String,
  targetDir: Path,
  files: Nel[Path]
)

object Options {
  val default: Opts[Options] =
    (Opts.option[Int]("crf", "The crf setting").withDefault(25),
      Opts.option[String]("ffmpeg", "The ffmpeg executable").withDefault("ffmpeg"),
      Opts.option[String]("ffprobe", "The ffprobe executable").withDefault("ffprobe"),
      Opts.option[Path]("target", "The target directory").withDefault(Path(".")),
      Opts.arguments[Path]("files")
    ).mapN(Options.apply)

  implicit def filesArgument: Argument[Path] =
    Argument[JPath].map(Path.fromNioPath)
}

/*

Input #0, matroska,webm, from '/mnt/nas/data/video/rip/Kaiserscharrndrama/Kaiserschmarrndrama_t00.mkv':
  Metadata:
    title           : Kaiserschmarrndrama
    encoder         : libmakemkv v1.16.5 (1.3.10/1.5.2) x86_64-unknown-linux-gnu
    creation_time   : 2022-01-10T15:23:18.000000Z
  Duration: 00:08:14.32, start: 0.000000, bitrate: 21710 kb/s
  Chapters:
    Chapter #0:0: start 0.000000, end 493.200000
      Metadata:
        title           : Chapter 01
    Chapter #0:1: start 493.200000, end 494.320000
      Metadata:
        title           : Chapter 02
  Stream #0:0(eng): Video: h264 (High), yuv420p(tv, bt709, top first), 1920x1080 [SAR 1:1 DAR 16:9], 25 fps, 25 tbr, 1k tbn, 50
 tbc
    Metadata:
      BPS-eng         : 21483950
      DURATION-eng    : 00:08:14.320000000
      NUMBER_OF_FRAMES-eng: 12358
      NUMBER_OF_BYTES-eng: 1327493307
      SOURCE_ID-eng   : 001011
      _STATISTICS_WRITING_APP-eng: MakeMKV v1.16.5 linux(x64-release)
      _STATISTICS_WRITING_DATE_UTC-eng: 2022-01-10 15:23:18
      _STATISTICS_TAGS-eng: BPS DURATION NUMBER_OF_FRAMES NUMBER_OF_BYTES SOURCE_ID
  Stream #0:1(deu): Audio: ac3, 48000 Hz, stereo, fltp, 224 kb/s (default)
    Metadata:
      title           : Stereo
      BPS-eng         : 224000
      DURATION-eng    : 00:08:14.304000000
      NUMBER_OF_FRAMES-eng: 15447
      NUMBER_OF_BYTES-eng: 13840512
      SOURCE_ID-eng   : 001100
      _STATISTICS_WRITING_APP-eng: MakeMKV v1.16.5 linux(x64-release)
      _STATISTICS_WRITING_DATE_UTC-eng: 2022-01-10 15:23:18
      _STATISTICS_TAGS-eng: BPS DURATION NUMBER_OF_FRAMES NUMBER_OF_BYTES SOURCE_ID
  Stream #0:2(deu): Subtitle: hdmv_pgs_subtitle, 1920x1080
    Metadata:
      BPS-eng         : 4682
      DURATION-eng    : 00:00:01.020000000
      NUMBER_OF_FRAMES-eng: 2
      NUMBER_OF_BYTES-eng: 597
      SOURCE_ID-eng   : 001200
      _STATISTICS_WRITING_APP-eng: MakeMKV v1.16.5 linux(x64-release)
      _STATISTICS_WRITING_DATE_UTC-eng: 2022-01-10 15:23:18
      _STATISTICS_TAGS-eng: BPS DURATION NUMBER_OF_FRAMES NUMBER_OF_BYTES SOURCE_ID


 */

object Parsing {
  import cats.parse.{Parser => P, Parser0 => P0}

  def until(c: Char) = P.charsWhile(_ != c) <* P.char(c)

  private[this] val ws: P0[Unit] = P.charIn(" \t\r\n").rep0.void

  val preamble = (P.string("Input") ~ until(':') ~ ws).void

  val title = P.string("Metadata:") *> ws *> P.string("title") *> ws *> P.char(':') ~ until('\n')
    //*> ws *> P.string("title").void *> ws *>
//    P.char(':') *> ws) *> until('\n')

  val all = preamble *> title

  def parse(str: String) =
    all.parse(str)
}

@main
def main(args: String*) =
  Main.main(args.toArray)
