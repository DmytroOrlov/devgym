lazy val root = (project in file("."))
  .enablePlugins(PlayScala)
  .settings(
    name := "devgym",
    version := "1.0-SNAPSHOT",
    scalaVersion := "2.11.7",
    routesGenerator := InjectedRoutesGenerator
  )
  .settings(testSettings)

lazy val testSettings = Seq(
  libraryDependencies ++= Seq(
    "org.scalatest" %% "scalatest" % "2.2.6" % "test",
    "org.scalacheck" %% "scalacheck" % "1.12.5" % "test",
    "org.scalatestplus" %% "play" % "1.4.0" % "test",
    "org.scalamock" %% "scalamock-scalatest-support" % "3.2.2" % "test"
  )
)