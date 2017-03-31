#!/usr/bin/env amm

import $ivy.`co.fs2::fs2-core:0.9.4`
import $ivy.`co.fs2::fs2-io:0.9.4`
import $ivy.`org.tukaani:xz:1.6`

import java.nio.file.{Paths, Path, Files}
import java.time._
import java.time.format._
import org.tukaani.xz.XZInputStream
import scala.sys.process._

import collection.mutable.ArrayBuffer
import fs2.{io, text, Chunk, Handle, Pipe, Pull, Stream, Task}
import scalaj.http._

import scala.util.Try

import data._


object settings {
  private def propertyPath(name: String): Option[Path] =
    Option(System.getProperty(name)).
      map(s => Paths.get(s))

  private def base =
    propertyPath("user.home").
      filter(p => Files.exists(p)).
      orElse(propertyPath("java.io.tmpdir")).
      filter(p => Files.exists(p)).
      getOrElse(sys.error("No directory found to store cache file"))

  val configDir: Path =
    propertyPath("meth.directory") match {
      case Some(p) if Files.exists(p) => p
      case Some(p) =>
        Files.createDirectories(p.getParent)
        p
      case None =>
        val dir = base.resolve(".config").resolve("meth")
        Files.createDirectories(dir)
        dir
    }

  val cacheFile = configDir.resolve("meth-cache.db")

  val movieFileXz = configDir.resolve("filmlist.xz")
  val movieFile = configDir.resolve("filmlist")
}

object data {

  case class Header1(
    createdUtc: String,
    createdLocal: String,
    listVersion: String,
    crawler: String)

  final class Show(fields: Seq[String]) {
    require(fields.size == 20, s"fields is $fields")

    // (Sender, Thema, Titel, Datum, Zeit, Dauer, Größe [MB],
    // Beschreibung, Url, Website, Url Untertitel, Url RTMP, Url
    // Klein, Url RTMP Klein, Url HD, Url RTMP HD, DatumL, Url
    // History, Geo, neu)

    lazy val station: String = fields(0)
    lazy val subject: String = fields(1)
    lazy val title: String = fields(2)
    lazy val date: Option[LocalDate] = Show.parseDate(fields(3))
    lazy val time: Option[LocalTime] = Show.parseTime(fields(4))
    lazy val duration: Option[Duration] = Show.parseDuration(fields(5))
    lazy val sizeMb: Option[Int] = Try(fields(6).toInt).toOption
    lazy val description: String = fields(7)
    lazy val url = fields(8)
    lazy val website = fields(9)
    lazy val subtitleUrl = fields(10)
    lazy val rtmpUrl = fields(11)
    lazy val smallUrl = fields(12)
    lazy val rtmpSmallUrl = fields(13)
    lazy val hdUrl = fields(14)
    lazy val rtmpHdUrl = fields(15)
    lazy val dateTime: Option[Instant] = Try(Instant.ofEpochSecond(fields(16).toLong)).toOption
    lazy val historyUrl = fields(17)
    lazy val geo = fields(18)
    lazy val isNew = fields(19).toBoolean

    def setStation(s: String): Show =
      if (s == station) this
      else new Show(s +: fields.tail)

    def fileName = Show.normalizeFilename(s"$subject-$title${date.map(d => "-"+d).getOrElse("")}.mp4")

    def asString = s"[$station] $subject: $title (${duration.map(_.toString).getOrElse("-:-:-")}; ${dateTime.map(_.toString).getOrElse("")})"
    override def toString(): String = fields.mkString("(",",",")")
  }

  object Show {
    private val dateF = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    private val timeF = DateTimeFormatter.ofPattern("HH:mm:ss")

    def normalizeFilename(s: String) = s.toLowerCase
      .replace(" - ", "-")
      .replaceAll("\\s+", "_")
      .replace("ä", "ae").replace("Ä", "Ae")
      .replace("ü", "ue").replace("Ü", "Ue")
      .replace("ö", "oe").replace("Ö", "Oe")
      .replace("ß", "ss")
      .replaceAll("[^a-zA-Z\\-_0-9\\.]", "")

    def parseDate(d: String) =
      if (d.trim.isEmpty) None
      else Some(LocalDate.parse(d.trim, dateF))

    def parseTime(t: String) =
      if (t.trim.isEmpty) None
      else Some(LocalTime.parse(t.trim, timeF))

    def parseDuration(d: String): Option[Duration] = {
      val parts = d.split(":")
      if (parts.length == 3) Try(Duration.ZERO.
        plusHours(parts(0).toLong).
        plusMinutes(parts(1).toLong).
        plusSeconds(parts(2).toLong)).toOption
      else None
    }
  }
}

object movielist {
  val lastmodFormat = DateTimeFormatter.RFC_1123_DATE_TIME
  val listUrl = "http://download10.onlinetvrecorder.com/mediathekview/Filmliste-akt.xz"

  def isListCurrent(target: Path): Task[Boolean] = Task.delay {
    lazy val fileLastmod = Files.getLastModifiedTime(target).toInstant

    def isCurrent(resp: HttpResponse[_]): Boolean = {
      val lastmod = resp.header("Last-Modified").map(lastmodFormat.parse).map(Instant.from)
      val size = resp.header("Content-Length")
      (lastmod, size, Files.exists(target)) match {
        case (Some(ts), Some(len), true) =>
          len.toLong == Files.size(target) && ts.isBefore(fileLastmod)
        case _ => false
      }
    }

    Files.exists(target) &&
    (fileLastmod.plusSeconds(60 * 60).isAfter(Instant.now) || isCurrent(Http(listUrl).method("HEAD").asString))
  }

  def downloadList(target: Path): Task[Path] = Task.delay {
    println("Downloading movie list…")
    Files.deleteIfExists(target)
    Http(listUrl).execute(in => {
      Files.copy(in, target)
    })
    target
  }

  def unpackFile(infile: Path, outfile: Path): Task[Path] = Task.delay {
    println("Unpacking movie list file…")
    val in = new XZInputStream(Files.newInputStream(infile))
    Files.deleteIfExists(outfile)
    Files.copy(in, outfile)
    outfile
  }

  def readFile: Stream[Task, String] = {
    val file = for {
      currentXz <- isListCurrent(settings.movieFileXz)
      file <- currentXz match {
        case true if Files.exists(settings.movieFile) =>
          Task.now(settings.movieFile)
        case true => unpackFile(settings.movieFileXz, settings.movieFile)
        case false =>  downloadList(settings.movieFileXz).flatMap(f => unpackFile(f, settings.movieFile))
      }
    } yield Files.newInputStream(file)
    io.readInputStream[Task](file, 8192).through(text.utf8Decode)
  }

  def get: Stream[Task, Show] =
    readFile.through(customparse.tokens).
      drop(2). // skip headers
      filter(_.size == 20). //skip errors in parser
      map(x => new Show(x)).
      through(moveStation)

  /** Copy the station from previous show, if current one is empty */
  def moveStation: Pipe[Task, Show, Show] = {
    s => s.mapAccumulate("")({
      case (_, show) if show.station.nonEmpty => (show.station, show)
      case (s, show) => (s, show.setStation(s))
    }).map(_._2)
  }

  object customparse {
    def nextString(in: String): Option[(String, String)] = {
      @annotation.tailrec
      def indexOf(s: String, start: Int = 0): Int =
        s.indexOf('"', start) match {
          case -1 => -1
          case n =>
            if (n > 0 && s.charAt(n-1) == '\\') indexOf(s, n+1)
            else n
        }

      indexOf(in) match {
        case -1 => None
        case n => indexOf(in, n+1) match {
          case -1 => None
          case k => Some((in.substring(n+1, k), in.substring(k+1)))
        }
      }
    }

    def allStrings(in: String): (Seq[String], String) = {
      @annotation.tailrec
      def loop(s: String, result: ArrayBuffer[String]): (ArrayBuffer[String], String) =
        if (s.indexOf(']') >= 0 && s.indexOf(']') < s.indexOf('"')) (result, s)
        else nextString(s) match {
          case Some((s, rem)) => loop(rem, result += s)
          case None => (result, s)
        }

      loop(in, ArrayBuffer.empty)
    }

    def nextArray(in: String): Option[(Seq[String], String)] = {
      in.indexOf('[') match {
        case -1 => None
        case n =>
          val (seq, rem) = allStrings(in.substring(n+1))
          rem.indexOf(']') match {
            case -1 => None
            case k => Some((seq, rem.substring(k+1)))
          }
      }
    }

    def allArrays(in: String): (Seq[Seq[String]], String) = {
      @annotation.tailrec
      def loop(s: String, result: ArrayBuffer[Seq[String]]): (ArrayBuffer[Seq[String]], String) =
        nextArray(s) match {
          case Some((array, rem)) => loop(rem, result += array)
          case None => (result, s)
        }
      loop(in, ArrayBuffer.empty)
    }

    def tokens: Pipe[Task, String, Seq[String]] = {
      def go(remaining: String = ""): Handle[Task, String] => Pull[Task, Seq[String], Unit] =
        _.receiveOption {
          case Some((chunk, h)) =>
            val string = chunk.foldLeft(remaining)(_ + _)
            val (ts, rem) = allArrays(string)
            Pull.output(Chunk.seq(ts)) >> go(rem)(h)
          case None if remaining.nonEmpty =>
            val (ts, rem) = allArrays(remaining)
            Pull.output(Chunk.seq(ts)) >> Pull.done
          case None =>
            Pull.done
        }

      _.pull(go(""))
    }
  }
}

case class Filter(f: Show => Boolean) {
  def &&(n: Filter): Filter = Filter(show => f(show) && n.f(show))
  def ||(n: Filter): Filter = Filter(show => f(show) || n.f(show))
  def negate: Filter = Filter(show => !f(show))
  def unary_! : Filter = negate
}

object Filter {
  val TRUE = Filter(_ => true)

  def longerThanMin(n: Int): Filter = Filter { s =>
    s.duration.exists(_.toMinutes > n)
  }

  def shorterThanMin(n: Int): Filter = Filter { s =>
    s.duration.exists(_.toMinutes < n)
  }

  def stationIs(name: String): Filter = Filter { s =>
    s.station.toLowerCase == name.toLowerCase
  }

  def subjectIs(w: String): Filter =
    Filter(s => s.subject.toLowerCase == w.toLowerCase)

  def dayIs(dow: DayOfWeek): Filter = Filter { s =>
    s.date.map(_.getDayOfWeek) == Some(dow)
  }

  def titleContains(w: String): Filter = Filter { s =>
    s.title.toLowerCase.contains(w.toLowerCase)
  }

  def contains(name: String): Filter = Filter({ s =>
    val nameL = name.toLowerCase
    s.title.toLowerCase.contains(nameL) ||
    s.subject.toLowerCase.contains(nameL)
  })

  def query(q: Seq[String]): Filter =
    q.foldLeft(TRUE) { (f, s) =>
      f && (s match {
        case w if w.startsWith("station:-") => stationIs(w.substring(9).trim).negate
        case w if w.startsWith("station:") => stationIs(w.substring(8).trim)
        case w if w.startsWith("subject:-") => subjectIs(w.substring(9).trim).negate
        case w if w.startsWith("subject:") => subjectIs(w.substring(8).trim)
        case w if w.startsWith("<") => shorterThanMin(w.substring(1).toInt)
        case w if w.startsWith(">") => longerThanMin(w.substring(1).toInt)
        case w if w.startsWith("!") => contains(w.substring(1)).negate
        case w => contains(w)
      })
    }
}




/** Download a show using curl into `target' directory. */
def curl(target: Path)(show: Show): Unit = {
  val out = target.resolve(show.fileName)
  if (Files.exists(out)) println(s"$out already exists")
  else {
    Files.createDirectories(out.getParent)
    println(s"Download '${show.title}' to $out …")
    Process(Seq("curl", "-L", "-o", out.toString, show.url), Some(target.toFile)).!!
  }
}

/** Download results of applying `filter'. */
def download(target: Path, filter: Filter): Unit = {
  movielist.get.
    filter(filter.f).
    evalMap(show => Task.delay(curl(target)(show))).
    run.
    unsafeRun
}

/** Print search results */
@main
def search(q: String*): Unit = {
  movielist.get.
    filter(Filter.query(q).f).
    evalMap(s => Task.delay(println(s.asString))).
    run.
    unsafeRun
}

/** Download search results */
@main
def download(q: String*): Unit = download(Paths.get("").toAbsolutePath, Filter.query(q))
