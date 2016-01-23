lazy val root = (project in file("."))
  .enablePlugins(PlayScala)
  .settings(
    name := "devgym",
    version := "1.0-SNAPSHOT",
    scalaVersion := "2.11.7",
    routesGenerator := InjectedRoutesGenerator
  )

libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.6" % "test"
