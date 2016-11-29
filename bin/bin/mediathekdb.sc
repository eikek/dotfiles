#!/usr/bin/env amm

// Creates a sqlite database of current mediathek movies

import $ivy.`com.h2database:h2:1.4.193`
import $ivy.`org.tukaani:xz:1.6`
import java.io.{InputStreamReader, BufferedReader}
import org.tukaani.xz.XZInputStream
import java.net.URL
import java.nio.file.Files
import java.nio.charset.StandardCharsets
import java.time._
import java.time.format._
import java.sql.{Connection, DriverManager, PreparedStatement}
import ammonite.ops._
import scalaj.http._
import upickle.Js
import scala.util.Try

val lastmodFormat = DateTimeFormatter.RFC_1123_DATE_TIME
val listUrl = "http://download10.onlinetvrecorder.com/mediathekview/Filmliste-akt.xz"

val tmp = Path(System.getProperty("java.io.tmpdir"))
val defaultListTarget = tmp/"filmlist.xz"
val defaultDbTarget = tmp/"filmlist.h2.db"

//"Filmliste" : [ "Sender", "Thema", "Titel", "Datum", "Zeit",
//  "Dauer", "Größe [MB]", "Beschreibung", "Url", "Website", "Url
//  Untertitel", "Url RTMP", "Url Klein", "Url RTMP Klein", "Url HD",
//  "Url RTMP HD", "DatumL", "Url History", "Geo", "neu" ]
// Bsp: "X" : [
//  "3Sat", "3sat", "Ein Eingang für die Ewigkeit © Alina Rapoport,
//  ZDF", "26.11.2016", "21:45:00", "00:44:39", "767", "\"Architektur
//  muss eine Vision haben, wie die Gesellschaft sein soll\", sagt
//  David Chipperfield. Der britische Stararchitekt baut gerade an
//  einem neuen Berliner Wah rzeichen: der...",
//  "http://nrodl.zdf.de/none/3sat/16/11/161126_doku_eingang_online/1/161126_doku_eingang_online_2328k_p35v13.mp4",
//  "http://www.3s at.de/mediathek/?mode=play&obj=62844", "", "",
//  "7|rodl.zdf.de/none/3sat/16/11/161126_doku_eingang_online/1/161126_doku_eingang_online_476k_p9v13. mp4",
//  "", "92|3328k_p36v13.mp4", "", "1480193100", "", "", "false" ],

case class Show(
  station: String,
  subject: String,
  title: String,
  date: Option[LocalDate],
  time: Option[LocalTime],
  duration: Option[Duration],
  sizeMb: Option[Int],
  description: String,
  url: String,
  website: String,
  url_sub: String,
  url_rtmp: String,
  url_small: String,
  url_small_rtmp: String,
  url_hd: String,
  url_hd_rtmp: String,
  dateL: Option[Instant],
  url_history: String,
  geo: String,
  newItem: Boolean) {


  def fileName = Show.normalizeFilename(s"$subject-$title${date.map(d => "-"+d).getOrElse("")}.mp4")
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
    if (parts.length == 3) Try(Duration.ZERO.plusHours(parts(0).toInt).plusMinutes(parts(1).toInt).plusSeconds(parts(2).toInt)).toOption
    else None
  }

  def fromJson(js: Js.Value): Option[Show] =
    js match {
      case Js.Arr(Js.Str(a), Js.Str(b), Js.Str(c), Js.Str(d), Js.Str(e), Js.Str(f), Js.Str(g), Js.Str(h), Js.Str(i), Js.Str(j), Js.Str(k), Js.Str(l), Js.Str(m), Js.Str(n), Js.Str(o), Js.Str(p), Js.Str(q), Js.Str(r), Js.Str(s), Js.Str(t)) =>
        Some(Show(a, b, c,
          parseDate(d),
          parseTime(e),
          parseDuration(f),
          Try(g.toInt).toOption,
          h, i, j, k, l, m, n, o, p,
          Try(Instant.ofEpochSecond(q.toLong)).toOption,
          r, s,
          t == "true"))
      case _ => None
    }


  def sonntagsTatort(s: Show): Boolean =
    s.station == "ARD" &&
    s.subject.toLowerCase == "tatort" &&
    !s.title.toLowerCase.contains("hörfassung") &&
    s.date.map(_.getDayOfWeek) == Some(DayOfWeek.SUNDAY) &&
    s.duration.map(_.toMinutes > 60) == Some(true)

  def unserSandmann(s: Show): Boolean =
    s.station == "ARD" &&
    s.title.toLowerCase.contains("sandmännchen") &&
    s.duration.map(_.toMinutes < 9) == Some(true)
}

def downloadList(target: Path = defaultListTarget): Path = {
  def isCurrent(resp: HttpResponse[_]): Boolean = {
    val lastmod = resp.header("Last-Modified").map(lastmodFormat.parse).map(Instant.from)
    val size = resp.header("Content-Length")
    (lastmod, size, exists(target)) match {
      case (Some(ts), Some(len), true) =>
        len.toLong == target.size && ts.isBefore(target.mtime.toInstant)
      case _ => false
    }
  }

  val head = Http(listUrl)
    .method("HEAD")
    .asString

  if (isCurrent(head)) {
    println("Movie list is current")
    target
  } else {
    println("Downloading current movie list…")
    rm(target)
    Http(listUrl).execute(in => {
      Files.copy(in, target.toNIO)
    })
    target
  }
}


def parseLine(line: String): Option[Js.Value] =
  line.trim.startsWith(""""X" : [""") match {
    case true =>
      val s = line.trim.substring(6)
      if (line.endsWith(",")) Some(upickle.json.read(s.dropRight(1)))
      else Some(upickle.json.read(s))
    case false => None
  }

def readXzShows(file: Path): Traversable[Show] = new Traversable[Show] {
  def foreach[U](f: Show => U): Unit = {
    val xzin = new XZInputStream(Files.newInputStream(file.toNIO))
    val r = new BufferedReader(new InputStreamReader(xzin, StandardCharsets.UTF_8))
    try {
      var line: String = r.readLine
      var ref: Option[Show] = None
      while (line != null) {
        val show = parseLine(line).flatMap(Show.fromJson)
          .map(s => ref match {
            case Some(ls) =>
              s.copy(station = if (s.station.isEmpty) ls.station else s.station)
               .copy(subject = if (s.subject.isEmpty) ls.subject else s.subject)
            case None => s
          })
        ref = show
        show.foreach(f)
        line = r.readLine
      }
    } finally {
      r.close
    }
  }
}

case class State(count: Int, conn: Connection, pst: PreparedStatement)

def openH2Db(file: Path): State = {
  Class.forName("org.h2.Driver");
  val conn = DriverManager.getConnection(s"jdbc:h2:${file.toNIO}", "sa", "");
  val stmt = conn.createStatement
  stmt.execute("drop table if exists movielist")
  stmt.execute("""create table movielist(
    station varchar(500),
    subject varchar(500),
    title varchar(500),
    date varchar(500),
    time varchar(500),
    duration integer,
    sizeMb integer,
    description varchar(500),
    url varchar(500),
    website varchar(500),
    url_sub varchar(500),
    url_rtmp varchar(500),
    url_small varchar(500),
    url_small_rtmp varchar(500),
    url_hd varchar(500),
    url_hd_rtmp varchar(500),
    dateL varchar(500),
    url_history varchar(500),
    geo varchar(500),
    newItem integer)""")
  stmt.close
  val p = conn.prepareStatement(s"insert into movielist values (${List.fill(20)("?").mkString(",")})")
  State(0, conn, p)
}

def writeToDatabase(state: State, show: Show): State = {
  def ostr[A](o: Option[A]): String = o.map(_.toString).getOrElse("null")

  def setOpt[A](idx: Int, s: Option[A]): Unit =
    s.map(d => state.pst.setString(idx, d.toString)).getOrElse(state.pst.setNull(idx, 0)) //0-NULL, 12-VARCHAR

  val p = state.pst
  p.setString( 1, show.station)
  p.setString( 2, show.subject)
  p.setString( 3, show.title)
  setOpt(4, show.date)
  setOpt(5, show.time)
  show.duration.map(n => p.setLong(6, n.toMillis)).getOrElse(p.setNull(6, 0))
  show.sizeMb.map(n => p.setInt(7, n)).getOrElse(p.setNull(7, 0))
  p.setString( 8, show.description)
  p.setString( 9, show.url)
  p.setString(10, show.website)
  p.setString(11, show.url_sub)
  p.setString(12, show.url_rtmp)
  p.setString(13, show.url_small)
  p.setString(14, show.url_small_rtmp)
  p.setString(15, show.url_hd)
  p.setString(16, show.url_hd_rtmp)
  setOpt(17, show.dateL)
  p.setString(18, show.url_history)
  p.setString(19, show.geo)
  p.setBoolean(20, show.newItem)
  p.executeUpdate()
  state.copy(count = state.count + 1)
}


// “public” interface

def mediathekDB(target: Path = defaultDbTarget): Path = {
  val xz = downloadList()
  if (!exists(target) || target.mtime.toInstant.isBefore(xz.mtime.toInstant)) {
    val zero = openH2Db(target)
    try {
      readXzShows(xz).foldLeft(zero)(writeToDatabase)
      zero.conn.createStatement.execute("create index station_idx on movielist(station)")
      zero.conn.createStatement.execute("create index subject_idx on movielist(subject)")
      zero.conn.createStatement.execute("create index url_idx on movielist(url)")
      zero.conn.createStatement.execute("create index date_idx on movielist(dateL)")
    } finally {
      zero.conn.close
    }
  }
  target
}


def allShows: Traversable[Show] =
  readXzShows(downloadList())


@main
def search: Unit = {
  println {
    allShows
      .withFilter(Show.unserSandmann)
      .map(_.url)
      .take(20)
  }
}

@main
def mkdb(target: Path): Unit = {
  mediathekDB(target/"movies.h2")
}
