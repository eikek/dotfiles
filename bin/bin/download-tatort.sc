#!/usr/bin/env amm

/*
 * Download the “Tatort“ movie of last sunday.
 */

import ammonite.ops._
import scalaj.http._
import java.time._

val movieListUrl = "http://mediathekdirekt.de/good.json"

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

  def fileName = (show+"-"+title+"-"+date+".mp4").toLowerCase
    .replaceAll("\\s+", "_")
    .replaceAll("ä", "ae").replaceAll("Ä", "Ae")
    .replaceAll("ü", "ue").replaceAll("Ü", "Ue")
    .replaceAll("ö", "oe").replaceAll("Ö", "Oe")
    .replaceAll("[^a-zA-Z\\-_0-9\\.]", "")
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
def main(target: Path, n: Int = 1): Unit = {
  implicit val wd = target
  movieList.filter(tatortFilter).take(n).foreach { show =>
    println(s"Download '${show.title}' to ${target/show.fileName} …")
    %curl ("-L", "-o", (target/show.fileName).toString, show.url)
  }
}

@main
def test(n: Int = 1): Unit = {
  println(movieList.filter(tatortFilter).take(n).toList)
}
