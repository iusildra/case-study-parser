import scala.sys.process._
import laika.rewrite.link.LinkConfig
import laika.rewrite.link.ApiLinks
import laika.theme.Theme

// https://typelevel.org/sbt-typelevel/faq.html#what-is-a-base-version-anyway
ThisBuild / tlBaseVersion := "0.0" // your current series x.y

ThisBuild / organization := "creativescala"
ThisBuild / organizationName := "Creative Scala"
ThisBuild / startYear := Some(2022)
ThisBuild / licenses := Seq(License.Apache2)
ThisBuild / developers := List(
  // your GitHub handle and name
  tlGitHubDev("noelwelsh", "Noel Welsh")
)

ThisBuild / tlSonatypeUseLegacyHost := true

// lazy val scala213 = "2.13.11"
lazy val scala3 = "3.3.0"

// ThisBuild / crossScalaVersions := Seq(scala213, scala3)
ThisBuild / scalaVersion := scala3 // the default Scala
ThisBuild / scalafixDependencies += "com.github.liancheng" %% "organize-imports" % "0.6.0"
ThisBuild / semanticdbEnabled := true
ThisBuild / semanticdbVersion := scalafixSemanticdb.revision

// Run this (build) to do everything involved in building the project
commands += Command.command("build") { state =>
  "dependencyUpdates" ::
    "compile" ::
    "test" ::
    "scalafixAll" ::
    "scalafmtAll" ::
    state
}

lazy val root = project
  .in(file("."))
  .settings(
    name := "case-study-parser",
    libraryDependencies ++= Seq(
      "org.typelevel" %%% "cats-core" % "2.10.0",
      "org.typelevel" %%% "cats-effect" % "3.5.1",
      ("org.scalameta" %% "scalameta" % "4.8.10").cross(CrossVersion.for3Use2_13),
      "org.scalameta" %%% "munit" % "0.7.29" % Test,
      "org.typelevel" %%% "munit-cats-effect-3" % "1.0.7" % Test,
      "qa.hedgehog" %%% "hedgehog-core" % "0.10.1" % Test,
      "qa.hedgehog" %%% "hedgehog-runner" % "0.10.1" % Test,
      "qa.hedgehog" %%% "hedgehog-munit" % "0.10.1" % Test,
      "qa.hedgehog" %%% "hedgehog-sbt" % "0.10.1" % Test,
    )
  )
