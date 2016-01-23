lazy val root = (project in file("."))
  .enablePlugins(PlayScala)
  .settings(testSettings: _*)
  .settings(
    name := "devgym",
    version := "1.0-SNAPSHOT",
    scalaVersion := "2.11.7",
    routesGenerator := InjectedRoutesGenerator,
    mappings in Universal ++=
      (baseDirectory.value / "test" / "tests" * "*" get) map
        (x => x -> ("tests/" + x.getName))
  )

lazy val testSettings = Seq(
  libraryDependencies ++= Seq(
    "org.scalatest" %% "scalatest" % "2.2.6" % "test",
    "org.scalacheck" %% "scalacheck" % "1.12.5" % "test",
    "org.scalatestplus" %% "play" % "1.4.0" % "test",
    "org.scalamock" %% "scalamock-scalatest-support" % "3.2.2" % "test"
  )
)
