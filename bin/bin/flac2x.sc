#!/usr/bin/env nix-shell
#! nix-shell -i amm -p mediainfo -p libav

import $ivy.`co.fs2::fs2-core:0.10.3`

import fs2._
import cats._, cats.effect._, cats.implicits._
import ammonite.ops._

@main
def main(in: Path @doc("A single music file or a path")
  , out: Path @doc("A path to put the created files into. Maybe a single file if `in` is, too.")
  , parallel: Boolean @doc("Whether to convert using all cores") = false
  , ogg: Boolean @doc("Convert all to ogg (16/44). Default is to flac, resampling to 16/44 if necessary.") = false
  , oggQuality: Int @doc("If `ogg` is used, this is the quality for encoding") = 8): Unit = {
  implicit val wd = pwd

  if (!exists(in)) {
    sys.error(s"Directory or file $in doesn't exist")
  }

  val task: Stream[IO, (MediaInfo, Path)] =
    createMainTask[IO](in, out, ogg, oggQuality)

  task.
    attempt.
    to(printSink).
    compile.drain.unsafeRunSync
  // val in = pwd/"test.flac"
  // val out = pwd/"test.ogg"
  // val out2 = pwd/"test-resampled.flac"

  // val conv = for {
  //   fi  <- MediaInfo[IO](in)
  //   _   <- fi.toFlac[IO](out2)
  // } yield ()
  // conv.unsafeRunSync
}

def createMainTask[F[_]: Sync](in: Path
  , out: Path
  , ogg: Boolean
  , quality: Int)(implicit wd: Path): Stream[F, (MediaInfo, Path)] =
  if (stat(in).isDir) {
    Stream.eval(Sync[F].delay(mkdir.!(out))) >>
    Stream.emit(in).covary[F].
      through(listFiles).
      filter(inputFilter).
      evalMap(MediaInfo.apply[F]).
      evalMap({ mi =>
        val outFile = makeOutFile(mi, in, out, ogg)
        checkOutFile(outFile).map(_ => (mi, outFile))
      }).
      evalMap({ case (mi, outFile) =>
        val conv =
          if (ogg) mi.toOgg(outFile, quality)
          else mi.toFlac(outFile)
        conv.map(_ => (mi, outFile))
      })
  } else {
    Stream.eval {
      for {
        mi      <- MediaInfo(in)
        outFile <- Sync[F].delay(makeOutFile(mi, in, out, ogg))
        _       <- checkOutFile(outFile)
        _       <- Sync[F].delay(print(s"Converting ${mi.file} -> $outFile"))
        _       <- if (ogg) mi.toOgg(outFile, quality) else mi.toFlac(outFile)
      } yield (mi, outFile)
    }
  }

def inputFilter(in: Path): Boolean =
  stat(in).isFile && MediaInfo.Format.all.contains(in.ext.toLowerCase)

def makeOutFile(in: MediaInfo, inPath: Path, out: Path, ogg: Boolean): Path = {
  // assume concrete file if ext is nonEmpty
  if (out.ext.nonEmpty) out
  else {
    val rel = in.file.relativeTo(inPath) match {
      case p if p == RelPath.empty => p
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
      ///err.printStackTrace()
      println(s"Skipped: ${err.getMessage}")
  })

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

  def toOgg[F[_]: Sync](out: Path, quality: Int)(implicit wd: Path): F[Unit] = Sync[F].delay {
    mkdir.!(out/up)
    if (format.lossy) cp(file, out)
    else audioFilter match {
      case Some(af) =>
        %("avconv", "-i", file
          , "-vn", "-sn"
          , "-af", s"aformat=$af"
          , "-map_metadata:s:a", "0:s:0"
          , "-codec:a", "libvorbis"
          , "-f", "ogg"
          , "-q:a", quality
          , out)
      case None =>
        %("avconv", "-i", file
          , "-vn", "-sn"
          , "-map_metadata:s:a", "0:s:0"
          , "-codec:a", "libvorbis"
          , "-f", "ogg"
          , "-q:a", quality
          , out)
    }
  }

  def toFlac[F[_]](out: Path)(implicit wd: Path, F: Sync[F]): F[Unit] = {
    if (format.lossy) F.delay(mkdir.!(out/up)) *> F.delay(cp(file, out))
    else audioFilter match {
      case Some(af) =>
        val conv = F.delay {
          %("avconv", "-i", file
          , "-vn", "-sn"
          , "-af", s"aformat=$af"
          , "-map_metadata", "0:g"
            , out)
        }
        for {
          _  <- F.delay(println(s"Converting flac to 16/44: $this -> $out ..."))
          _  <- F.delay(mkdir.!(out/up))
          _  <- conv
          _  <- FlacOps.copyCover(this, out)
        } yield ()
      case None =>
        F.delay(println(s"Copying flac file to $out")) *>
        F.delay(mkdir.!(out/up)) *>
        F.delay(cp(file, out))
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
        Sync[F].delay(println("Flac2Ogg cover not implemented"))

      case _ =>
        Sync[F].delay(println(s"Cannot copy cover art from ${from.file} to ${to.file}"))
    }
  }
}

