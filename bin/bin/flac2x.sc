#!/usr/bin/env nix-shell
#! nix-shell -i amm -p mediainfo -p ffmpeg

// https://codecs.multimedia.cx/2011/10/why-ffmpeg-is-better-than-libav-by-numbers-but-not-in-reality/
// http://blog.pkh.me/p/13-the-ffmpeg-libav-situation.html
// using ffmpeg, not avconv

// The „get a picture into an ogg file” is from here
// https://github.com/biapy/howto.biapy.com/blob/master/various/mussync-tools

import $ivy.`co.fs2::fs2-core:0.10.3`
import $ivy.`org.apache.tika:tika-core:1.17`

import fs2._
import cats._, cats.effect._, cats.implicits._
import org.apache.tika.config.TikaConfig
import scopt.Read
import ammonite.ops._

sealed trait FileAction
object FileAction {
  case object Copy extends FileAction
  case object Symlink extends FileAction
  case object Hardlink extends FileAction
  case object Skip extends FileAction

  implicit val scoptRead: Read[FileAction] =
    Read.reads(s => s.toLowerCase match {
      case "copy" => Copy
      case "symlink" => Symlink
      case "hardlink" => Hardlink
      case "skip" => Skip
      case _ => sys.error(s"Invalid file action: $s. Use one of: copy,symlink,hardlink or skip")
    })
}

@main
def main(in: Path @doc("A single music file or a path")
  , out: Path @doc("A path to put the created files into. Maybe a single file if `in` is, too.")
  , parallel: Boolean @doc("Whether to convert using all cores") = false
  , rest: FileAction @doc("What to do with files that are already fine or lossy encoded.") = FileAction.Skip
  , ogg: Boolean @doc("Convert all to ogg (16/44). Default is to flac, resampling to 16/44 if necessary.") = false
  , oggQuality: Int @doc("If `ogg` is used, this is the quality for encoding") = 8): Unit = {
  implicit val wd = pwd

  if (!exists(in)) {
    sys.error(s"Directory or file $in doesn't exist")
  }

  val task: Stream[IO, Either[Throwable, (MediaInfo, Path)]] =
    createMainTask[IO](in, out, rest, ogg, oggQuality)

  task.
    to(printSink).
    compile.drain.unsafeRunSync
}

def createMainTask[F[_]: Sync](in: Path
  , out: Path
  , rest: FileAction
  , ogg: Boolean
  , quality: Int)(implicit wd: Path): Stream[F, Either[Throwable, (MediaInfo, Path)]] = {

  def convert(mi: MediaInfo): F[(MediaInfo, Path)] = {
    val outFile = makeOutFile(mi, in, out, ogg)
    val conv =
      if (ogg) mi.toOgg(outFile, quality, rest)
      else mi.toFlac(outFile, rest)
    for {
      _  <- checkOutFile(outFile)
      _  <- conv
    } yield (mi, outFile)
  }

  if (stat(in).isDir) {
    Stream.eval(Sync[F].delay(mkdir.!(out))) >>
    Stream.emit(in).covary[F].
      through(listFiles).
      filter(inputFilter).
      evalMap(file => MediaInfo(file).flatMap(convert).attempt)
  } else {
    Stream.eval {
      MediaInfo(in).flatMap(convert).attempt
    }
  }
}

def inputFilter(in: Path): Boolean =
  stat(in).isFile && MediaInfo.Format.all.contains(in.ext.toLowerCase)

def makeOutFile(in: MediaInfo, inPath: Path, out: Path, ogg: Boolean): Path = {
  // assume concrete file if ext is nonEmpty
  if (out.ext.nonEmpty) out
  else {
    val rel = in.file.relativeTo(inPath) match {
      case p if p.segments.isEmpty => p
      case p => p/up
    }
    if (in.format.lossy) {
      out/rel/in.file.last
    } else {
      val basename = in.file.last.substring(0, in.file.last.length - in.file.ext.length - 1)
      if (ogg) out/rel/(basename +".ogg")
      else out/rel/(basename + ".flac")
    }
  }
}

def checkOutFile[F[_]: Sync](file: Path): F[Unit] =
  Sync[F].delay {
    if (exists(file)) {
      sys.error(s"Output file $file already exists.")
    }
  }

def listFiles[F[_]]: Pipe[F, Path, Path] = _.flatMap { p =>
  val info = stat.full(p)
  if (info.isDir) Stream.emits(ls.!(p)).covary[F].through(listFiles[F])
  else Stream.emit(p)
}

def printSink[F[_]]: Sink[F, Either[Throwable, (MediaInfo, Path)]] =
  _.map({
    case Right((in, out)) => println(s"Transcode ${in.file} to $out")
    case Left(err) =>
      //err.printStackTrace()
      println(s"Skipped: ${err.getMessage}")
  })

def fileActionImpl[F[_]](in: Path, out: Path, action: FileAction)(implicit F: Sync[F]): F[Unit] = {
  F.delay {
    action match {
      case FileAction.Skip =>
        println(s"Skipping file $in")

      case FileAction.Copy =>
        println(s"Copying file $in to $out")
        mkdir.!(out/up)
        cp(in, out)

      case FileAction.Symlink =>
        println(s"Symlink file $in to $out")
        mkdir.!(out/up)
        ln.s(in, out)

      case FileAction.Hardlink =>
        println(s"Hardlink file $in to $out")
        mkdir.!(out/up)
        ln(in, out)
    }
  }
}


case class MediaInfo(
  file: Path
    , format: MediaInfo.Format
    , sampleRate: Int
    , bitDepth: Int
    , cover: Boolean
) {

  def audioFilter: Option[String] =
    (sampleRate, bitDepth) match {
      case (sr, bd) if sr > 44100 && bd > 16 =>
        Some(s"sample_fmts=s16:sample_rates=44100")
      case (sr, bd) if sr > 44100 =>
        Some("sample_rates=44100")
      case (sr, bd) if bd > 16 =>
        Some("sample_fmts=s16")
      case _ =>
        None
    }

  def toOgg[F[_]: Sync](out: Path, quality: Int, rest: FileAction)(implicit wd: Path): F[Unit] = {
    if (format.lossy) fileActionImpl(file, out, rest)
    else audioFilter match {
      case Some(af) =>
        Sync[F].delay {
          mkdir.!(out/up)
          %("ffmpeg", "-i", file
            , "-vn", "-sn"
            , "-af", s"aformat=$af"
            , "-map_metadata:s:a", "0:s:0"
            , "-codec:a", "libvorbis"
            , "-f", "ogg"
            , "-q:a", quality
            , out)
        }
      case None =>
        Sync[F].delay {
          mkdir.!(out/up)
          %("ffmpeg", "-i", file
            , "-vn", "-sn"
            , "-map_metadata:s:a", "0:s:0"
            , "-codec:a", "libvorbis"
            , "-f", "ogg"
            , "-q:a", quality
            , out)
        }
    }
  }

  def toFlac[F[_]](out: Path, rest: FileAction)(implicit wd: Path, F: Sync[F]): F[Unit] = {
    if (format.lossy) fileActionImpl(file, out, rest)
    else audioFilter match {
      case Some(af) =>
        val conv = F.delay {
          mkdir.!(out/up)
          %("ffmpeg", "-i", file
          , "-vn", "-sn"
          , "-af", s"aformat=$af"
          , "-map_metadata", "0:g"
            , out)
        }
        for {
          _  <- F.delay(println(s"Converting flac to 16/44: $this -> $out ..."))
          _  <- conv
          _  <- FlacOps.copyCover(this, out)
        } yield ()
      case None =>
        fileActionImpl(file, out, rest)
    }
  }
}

object MediaInfo {

  sealed trait Format {
    def lossless: Boolean
    def lossy = !lossless
  }

  object Format {
    val all: Set[String] = Set("wav", "flac", "mp3", "ogg")

    case object Wav extends Format {
      val lossless = true
    }
    case object Flac extends Format {
      val lossless = true
    }
    case object Mp3 extends Format {
      val lossless = false
    }
    case object Ogg extends Format {
      val lossless = false
    }

    def parse(s: String): Option[Format] =
      s.toLowerCase match {
        case "wav" => Wav.some
        case "flac" => Flac.some
        case "mp3" => Mp3.some
        case "ogg" => Ogg.some
        case _ => None
      }
  }

  def apply[F[_]: Sync](file: Path)(implicit wd: Path): F[MediaInfo] =
    Sync[F].delay {
      val bs = %%("mediainfo", "--Inform=Audio;%BitDepth%,%SamplingRate%", file).out.string.trim
      val cs = %%("mediainfo", "--Inform=General;%Cover%,%Format%", file).out.string.trim.toLowerCase

      val (bit, sample, cover, fmt) =
        (bs.split(',').toList ::: cs.split(',').toList) match {
          case bd :: sr :: cv :: fmt :: Nil =>
          (bd.toInt, sr.toInt, cv.toLowerCase == "yes", Format.parse(fmt).getOrElse(sys.error(s"Unknown format: $fmt")))
        case _ =>
          sys.error(s"Invalid output from mediainfo: `$bs` for file $file")
      }

      MediaInfo(file, fmt, sample, bit, cover)
    }
}

object FlacOps {
  import MediaInfo.Format._

  def copyCover[F[_]: Sync](from: MediaInfo, to: Path)(implicit wd: Path): F[Unit] = 
    MediaInfo[F](to).flatMap(outInfo => copyCover(from, outInfo))
  

  def copyCover[F[_]: Sync](from: MediaInfo, to: MediaInfo)(implicit wd: Path): F[Unit] = {
    if (!from.cover) Sync[F].delay(println(s"File ${from.file} has no cover"))
    else (from.format, to.format) match {
      case (Flac, Flac) =>
        Sync[F].delay {
          println(s"Copy cover from ${from.file} to ${to.file}")
          val coverFile = tmp()
          %("metaflac", "--export-picture-to", coverFile, from.file)
          %("metaflac", "--import-picture-from", coverFile, to.file)
          rm.!(coverFile)
        }
        
      case (Flac, Ogg) =>
        Sync[F].delay {
          println("sCannot copy cover art to ogg file: ${from.file} -> ${to.file}")
          // println(s"Copy cover from ${from.file} to ${to.file}")
          // val coverFile = tmp()
          // %("metaflac", "--export-picture-to", coverFile, from.file)

        }

      case _ =>
        Sync[F].delay(println(s"Cannot copy cover art from ${from.file} to ${to.file}"))
    }
  }
}

object Mimetype {
  private val tika = new TikaConfig().getDetector


  def from(path: Path): Option[String] =
    None
 
}

