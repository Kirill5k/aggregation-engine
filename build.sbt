ThisBuild / scalaVersion     := "2.13.4"
ThisBuild / version          := "0.1.0"
ThisBuild / organization     := "io.github.kirill5k"
ThisBuild / organizationName := "kirill5k"

lazy val noPublish = Seq(
  publish := {},
  publishLocal := {},
  publishArtifact := false,
  publish / skip := true
)

lazy val root = (project in file("."))
  .settings(noPublish)
  .settings(
    name := "aggregation-engine"
  )
  .aggregate(core)

lazy val core = (project in file("core"))
  .settings(
    name := "aggregation-engine-core",
    moduleName := "aggregation-engine-core",
    libraryDependencies ++= Dependencies.core ++ Dependencies.test
  )
