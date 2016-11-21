#!/usr/bin/env amm

/*
 * Download the “Tatort“ movie of last sunday.
 */

import scala.util.Try
import ammonite.ops._
import scalaj.http._
import java.time._

val movieListUrl = "http://mediathekdirekt.de/good.json"

def normalizeFilename(s: String) = s.toLowerCase
  .replaceAll(" - ", "-")
  .replaceAll("\\s+", "_")
  .replaceAll("ä", "ae").replaceAll("Ä", "Ae")
  .replaceAll("ü", "ue").replaceAll("Ü", "Ue")
  .replaceAll("ö", "oe").replaceAll("Ö", "Oe")
  .replaceAll("[^a-zA-Z\\-_0-9\\.]", "")

case class Show(station: String, title: String, show: String, date: String, time: String, url: String) {
  lazy val localDate: LocalDate = LocalDate.of(
    date.substring(6).toInt,
    date.substring(3, 5).toInt,
    date.substring(0, 2).toInt
  )
  lazy val duration: Duration = Duration
    .ofHours(time.substring(0, 2).toInt)
    .plusMinutes(time.substring(3, 5).toInt)
    .plusSeconds(time.substring(6).toInt)

  def showIs(name: String) = show.toLowerCase == name

  def fileName(db: TvDb) = db.fileName(this) getOrElse {
    normalizeFilename(show+"-"+title+"-"+date+".mp4")
  }
}

class TvDb(apikey: String, userkey: String, username: String) {
  val tatortId = 83214
  val thetvdbUrl = "https://api.thetvdb.com"
  val token: String = newToken()

  def makeUrl(show: Show): String =
    s"${thetvdbUrl}/series/$tatortId/episodes/query"

  def newToken(): String = {
    val res = Http(s"$thetvdbUrl/login")
      .postData(s"""{"apikey": "$apikey", "username": "$username", "userkey": "$userkey"}""")
      .header("content-type", "application/json")
      .asString
      .body

    val token = upickle.json.read(res).obj.apply("token").toString
    token.substring(1, token.length-1)
  }

  def request(url: String): HttpRequest = {
    Http(url)
      .header("Accept-Language", "de")
      .header("Authorization", s"Bearer ${token}")
  }

  def searchEpisode(show: Show): Option[Map[String, upickle.Js.Value]] = {
    Try({
      val req = request(makeUrl(show)).param("firstAired", show.localDate.toString)
      val json = upickle.json.read(req.asString.body).obj
      json.apply("data").arr(0).obj
    }).toOption
  }

  def fileName(show: Show): Option[String] = Try({
    searchEpisode(show).map { data =>
      s"${show.localDate.getYear}/s${show.localDate.getYear}e${data("airedEpisodeNumber")}_${normalizeFilename(data("episodeName").toString)}.mp4"
    }
  }).toOption.flatten

}

def movieList = upickle.json.read(Http(movieListUrl).asString.body).arr.toStream.map { item =>
  Show(
    item.arr(0).str,
    item.arr(1).str,
    item.arr(2).str,
    item.arr(3).str,
    item.arr(4).str,
    item.arr(6).str)
}

def tatortFilter(s: Show): Boolean =
  s.station == "ARD" &&
  s.showIs("tatort") &&
  !s.title.toLowerCase.contains("hörfassung") &&
  s.localDate.getDayOfWeek == DayOfWeek.SUNDAY &&
  s.duration.toHours >= 1

@main
def main(target: Path, apikey: String, userkey: String, username: String, n: Int = 1): Unit = {
  implicit val wd = target
  val db = new TvDb(apikey, userkey, username)
  movieList.filter(tatortFilter).take(n).foreach { show =>
    val out = target/RelPath(show.fileName(db))
    if (exists(out)) println(s"$out already exists")
    else {
      mkdir(out/up)
      println(s"Download '${show.title}' to $out …")
      %curl ("-L", "-o", out.toString, show.url)
    }
  }
}

@main
def test(n: Int = 1): Unit = {
  println(movieList.filter(tatortFilter).take(n).toList)
}
