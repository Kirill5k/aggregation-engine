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

lazy val docker = Seq(
  packageName := moduleName.value,
  version := version.value,
  maintainer := "immotional@aol.com",
  dockerBaseImage := "adoptopenjdk/openjdk15-openj9:debianslim-jre",
  dockerUpdateLatest := true,
  makeBatScripts := List()
)

lazy val root = (project in file("."))
  .settings(noPublish)
  .settings(
    name := "aggregation-engine"
  )
  .aggregate(core)

lazy val core = (project in file("core"))
  .enablePlugins(JavaAppPackaging, JavaAgent, DockerPlugin)
  .settings(docker)
  .settings(
    name := "aggregation-engine-core",
    moduleName := "aggregation-engine-core",
    libraryDependencies ++= Dependencies.core ++ Dependencies.test
  )
