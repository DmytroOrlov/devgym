val scalaVer = "2.11.7"

lazy val root = (project in file("."))
  .enablePlugins(PlayScala)
  .settings(runtimeSettings ++ testSettings)
  .settings(
    name := "devgym",
    version := "1.0-SNAPSHOT",
    scalaVersion := scalaVer,
    routesGenerator := InjectedRoutesGenerator,
    mappings in Universal ++=
      (baseDirectory.value / "test" / "tests" * "*").get map
        (x => x -> ("test/tests/" + x.getName))
  )

lazy val runtimeSettings = Seq(
  libraryDependencies ++= Seq(
    "org.webjars" % "jquery" % "2.2.0",

    "com.datastax.cassandra" % "cassandra-driver-core" % "3.0.0-rc1" exclude("org.xerial.snappy", "snappy-java"),
    "com.typesafe.scala-logging" %% "scala-logging" % "3.1.0",

    "org.scala-lang" % "scala-compiler" % scalaVer,
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
