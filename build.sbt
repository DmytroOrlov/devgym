lazy val root = (project in file("."))
  .enablePlugins(PlayScala)
  .settings(runtimeSettings)
  .settings(testSettings)
  .settings(
    name := "devgym",
    version := "1.0-SNAPSHOT",
    scalaVersion := "2.11.7",
    routesGenerator := InjectedRoutesGenerator,
    mappings in Universal ++=
      (baseDirectory.value / "test" / "tests" * "*").get map
        (x => x -> ("test/tests/" + x.getName))
  )

lazy val runtimeSettings = Seq(
  libraryDependencies ++= Seq(
    "org.scala-lang" % "scala-compiler" % scalaVersion.value,
    "org.scalatest" %% "scalatest" % "2.2.6"
  )
)

lazy val testSettings = Seq(
  libraryDependencies ++= Seq(
    "org.scalacheck" %% "scalacheck" % "1.12.5" % "test",
    "org.scalatestplus" %% "play" % "1.4.0" % "test",
    "org.scalamock" %% "scalamock-scalatest-support" % "3.2.2" % "test"
  )
)
