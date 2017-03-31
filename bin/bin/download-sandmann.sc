#!/usr/bin/env amm

/*
 * Download the “Sandmännchen“
 */

import ammonite.ops._
import fs2.Task
import $file.`mediathekdb2`
import mediathekdb2.Filter
import mediathekdb2.data.Show
import mediathekdb2.Filter._

def unserSandmann: Filter =
  stationIs("ARD") &&
  titleContains("sandmännchen") &&
  shorterThanMin(9)

@main
def main(target: Path, n: Int = 1): Unit = {
  mediathekdb2.movielist.get
    .filter(unserSandmann.f)
    .take(n)
    .evalMap(s => Task.delay(mediathekdb2.curl(target.toNIO)(s)))
    .run.unsafeRun
}

@main
def test(n: Int = 1): Unit = {
  mediathekdb2.movielist.get
    .filter(unserSandmann.f)
    .take(n)
    .map(_.asString)
    .evalMap(s => Task.delay(println(s)))
    .run.unsafeRun
}
