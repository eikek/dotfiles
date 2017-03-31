#!/usr/bin/env amm

/*
 * Download the “Tatort“ movie of last sunday.
 */

import scala.util.Try
import ammonite.ops._
import scalaj.http._
import java.time._
import fs2.Task
import $file.`mediathekdb2`
import mediathekdb2.Filter
import mediathekdb2.data.Show
import mediathekdb2.Filter._

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
      val req = request(makeUrl(show)).param("firstAired", show.date.get.toString)
      val json = upickle.json.read(req.asString.body).obj
      json.apply("data").arr.map(_.obj).find(obj => {
        obj.get("episodeName").map(_.str).exists(_.toLowerCase.contains(show.title.toLowerCase))
      })
    }).toOption.flatten
  }

  def fileName(show: Show): Option[String] = Try({
    val date = show.date.get
    searchEpisode(show).map { data =>
      s"${date.getYear}/s${date.getYear}e${data("airedEpisodeNumber")}_${Show.normalizeFilename(data("episodeName").toString)}.mp4"
    }
  }).toOption.flatten
}

def sonntagsTatort: Filter =
  stationIs("ARD") &&
  subjectIs("tatort") &&
  !titleContains("hörfassung") &&
  dayIs(DayOfWeek.SUNDAY) &&
  longerThanMin(60)

@main
def main(target: Path, apikey: String, userkey: String, username: String, n: Int = 1): Unit = {
  implicit val wd = target
  val db = new TvDb(apikey, userkey, username)
  mediathekdb2.movielist.get.filter(sonntagsTatort.f).take(n).evalMap(show => Task.delay {
    val out = target/RelPath(db.fileName(show).getOrElse(show.fileName))
    if (exists(out)) println(s"$out already exists")
    else {
      mkdir(out/up)
      println(s"Download '${show.title}' to $out …")
      %curl("-L", "-o", out.toString, show.url)
    }
  }).run.unsafeRun
}

@main
def test(n: Int = 1): Unit = {
  mediathekdb2.movielist.get
    .filter(sonntagsTatort.f)
    .take(n)
    .map(_.asString)
    .evalMap(s => Task.delay(println(s)))
    .run.unsafeRun
}
