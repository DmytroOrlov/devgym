lazy val root = (project in file("."))
  .enablePlugins(PlayScala)
  .settings(
    name := "devgym",
    version := "1.0-SNAPSHOT",
    scalaVersion := "2.11.6",
    routesGenerator := InjectedRoutesGenerator
  )
