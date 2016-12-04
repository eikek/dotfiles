#!/usr/bin/env amm

/*
 * Download the “Sandmännchen“
 */

import ammonite.ops._
import $file.`mediathekdb`
import mediathekdb.{Filter, Show}
import mediathekdb.Filter._

def unserSandmann: Filter =
  stationIs("ARD") &&
  titleContains("sandmännchen") &&
  shorterThanMin(9)

@main
def main(target: Path, n: Int = 1): Unit = {
  mediathekdb.allShows
    .filter(unserSandmann.f)
    .take(n)
    .foreach(mediathekdb.curl(target))
}

@main
def test(n: Int = 1): Unit = {
  mediathekdb.allShows
    .filter(unserSandmann.f)
    .take(n)
    .map(_.asString)
    .foreach(println)
}
