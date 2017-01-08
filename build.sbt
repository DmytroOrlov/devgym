import sbt.Keys._
import sbt.Project.projectToRef

val scalaV = "2.11.8"
val scalatestV = "2.2.6"
val akkaHttpV = "2.4.11"

lazy val commonSettings = Seq(scalaVersion := scalaV)

lazy val testSettings = Seq(
  libraryDependencies ++= Seq(
    "org.scalatest" %% "scalatest" % scalatestV % "test",
    "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % "test",
    "org.scalamock" %% "scalamock-scalatest-support" % "3.2.2" % "test",
    "com.storm-enroute" %% "scalameter-core" % "0.7" % "test"
  )
)

lazy val clients = Seq(client)

lazy val UnitTest = config("unit") extend Test

val monifuVer = "1.2"
val akkaV = "2.4.12"

lazy val server = (project in file("server"))
  .configs(UnitTest)
  .settings(inConfig(UnitTest)(Defaults.testTasks): _*)
  .enablePlugins(PlayScala)
  .aggregate(clients.map(projectToRef): _*)
  .dependsOn(sharedJvm)
  .settings(commonSettings ++ testSettings)
  .settings(
    includeFilter in(Assets, LessKeys.less) := "*.less",
    name := "devgym",
    version := "1.0-SNAPSHOT",
    testOptions in UnitTest += Tests.Argument("-l", "RequireDB"),
    scalaJSProjects := clients,
    pipelineStages := Seq(scalaJSProd, gzip),

    mappings in Universal ++=
      (baseDirectory.value / "test" / "tests" * "*").get map
        (x => x -> ("test/tests/" + x.getName)),

    libraryDependencies ++= Seq(
      cache,
      ws,

      "org.webjars" % "jquery" % "2.2.4",
      "org.webjars" % "bootstrap" % "3.3.6" exclude("org.webjars", "jquery"),
      "org.monifu" %% "monifu" % monifuVer,
      "com.datastax.cassandra" % "cassandra-driver-core" % "3.1.2"
        exclude("org.xerial.snappy", "snappy-java")
        exclude("com.google.guava", "guava"),
      "io.getquill" %% "quill-cassandra" % "1.0.0",

      "org.scala-lang" % "scala-compiler" % scalaV,
      "org.scalatest" %% "scalatest" % scalatestV,
      "org.scalameta" %% "scalameta" % "1.4.0"
    )
  )

lazy val client = (project in file("client"))
  .enablePlugins(ScalaJSPlugin, ScalaJSPlay)
  .dependsOn(sharedJs)
  .settings(commonSettings: _*)
  .settings(
    persistLauncher := false,
    persistLauncher in Test := false,
    libraryDependencies ++= Seq(
      "org.scala-js" %%% "scalajs-dom" % "0.9.1",
      "be.doeraene" %%% "scalajs-jquery" % "0.9.1",
      "org.monifu" %%% "monifu" % monifuVer
    )
  )

lazy val shared = (crossProject.crossType(CrossType.Pure) in file("shared"))
  .settings(commonSettings: _*)
  .jsConfigure(_ enablePlugins ScalaJSPlay)

lazy val sharedJvm = shared.jvm
lazy val sharedJs = shared.js

// loads the Play project at sbt startup
onLoad in Global := (Command.process("project server", _: State)) compose (onLoad in Global).value
