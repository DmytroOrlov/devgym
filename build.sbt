import sbt.Project.projectToRef

val scalaVer = "2.11.7"

lazy val commonSettings = Seq(scalaVersion := scalaVer)

lazy val testSettings = Seq(
  libraryDependencies ++= Seq(
    "org.scalatest" %% "scalatest" % "2.2.6" % "test",
    "org.scalacheck" %% "scalacheck" % "1.12.5" % "test",
    "org.scalatestplus" %% "play" % "1.4.0" % "test",
    "org.scalamock" %% "scalamock-scalatest-support" % "3.2.2" % "test"
  )
)

lazy val clients = Seq(client)

lazy val server = (project in file("server"))
  .enablePlugins(PlayScala)
  .aggregate(clients.map(projectToRef): _*)
  .dependsOn(sharedJvm)
  .settings(commonSettings ++ testSettings)
  .settings(
    name := "devgym",
    version := "1.0-SNAPSHOT",
    routesGenerator := InjectedRoutesGenerator,
    scalaJSProjects := clients,
    pipelineStages := Seq(scalaJSProd, gzip),

    mappings in Universal ++=
      (baseDirectory.value / "test" / "tests" * "*").get map
        (x => x -> ("test/tests/" + x.getName)),

    libraryDependencies ++= Seq(
      "org.webjars" % "jquery" % "2.2.0",
      "com.vmunier" %% "play-scalajs-scripts" % "0.4.0",

      "com.datastax.cassandra" % "cassandra-driver-core" % "3.0.0-rc1" exclude("org.xerial.snappy", "snappy-java"),
      "com.typesafe.scala-logging" %% "scala-logging" % "3.1.0", // todo remove

      "org.scala-lang" % "scala-compiler" % scalaVer,
      "org.scalatest" %% "scalatest" % "2.2.6"
    )
  )

lazy val client = (project in file("client"))
  .enablePlugins(ScalaJSPlugin, ScalaJSPlay)
  .dependsOn(sharedJs)
  .settings(commonSettings: _*)
  .settings(
    persistLauncher := true,
    persistLauncher in Test := false,
    libraryDependencies ++= Seq(
      "org.scala-js" %%% "scalajs-dom" % "0.8.2",
      "be.doeraene" %%% "scalajs-jquery" % "0.8.1",
      "org.monifu" %%% "monifu" % "1.0"
    )
  )

lazy val shared = (crossProject.crossType(CrossType.Pure) in file("shared"))
  .settings(commonSettings: _*)
  .jsConfigure(_ enablePlugins ScalaJSPlay)

lazy val sharedJvm = shared.jvm
lazy val sharedJs = shared.js

// loads the Play project at sbt startup
onLoad in Global := (Command.process("project server", _: State)) compose (onLoad in Global).value

scalaJSUseRhino in Global := false // please install nodejs
