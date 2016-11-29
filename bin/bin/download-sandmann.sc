#!/usr/bin/env amm

/*
 * Download the “Sandmännchen“
 */

import ammonite.ops._
import $file.`mediathekdb`
import mediathekdb.Show

@main
def main(target: Path, n: Int = 1): Unit = {
  implicit val wd = target
  mediathekdb.allShows.filter(Show.unserSandmann).take(n).foreach { show =>
    val out = target/RelPath(show.fileName)
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
  println(mediathekdb.allShows.filter(Show.unserSandmann).take(n).toList.mkString("\n"))
}
