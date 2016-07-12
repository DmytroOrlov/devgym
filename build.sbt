import sbt.Project.projectToRef

val scalaV = "2.11.8"
val scalatestV = "2.2.6"

lazy val commonSettings = Seq(scalaVersion := scalaV)

lazy val testSettings = Seq(
  libraryDependencies ++= Seq(
    "org.scalatest" %% "scalatest" % scalatestV % "test",
    "org.scalacheck" %% "scalacheck" % "1.13.0" % "test",
    "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.0" % "test",
    "org.scalamock" %% "scalamock-scalatest-support" % "3.2.2" % "test"
  )
)

lazy val clients = Seq(client)

lazy val UnitTest = config("unit") extend Test

val monifuVer = "1.1"

lazy val server = (project in file("server"))
  .configs(UnitTest)
  .settings(inConfig(UnitTest)(Defaults.testTasks): _*)
  .enablePlugins(PlayScala)
  .aggregate(clients.map(projectToRef): _*)
  .dependsOn(sharedJvm)
  .settings(commonSettings ++ testSettings)
  .settings(
    includeFilter in (Assets, LessKeys.less) := "*.less",
    name := "devgym",
    version := "1.0-SNAPSHOT",
    testOptions in UnitTest += Tests.Argument("-l",  "RequireDB"),
    scalaJSProjects := clients,
    pipelineStages := Seq(scalaJSProd, gzip),

    mappings in Universal ++=
      (baseDirectory.value / "test" / "tests" * "*").get map
        (x => x -> ("test/tests/" + x.getName)),

    libraryDependencies ++= Seq(
      cache,

      "org.webjars" % "jquery" % "2.2.2",
      "org.webjars" % "bootstrap" % "3.3.6" exclude("org.webjars", "jquery"),
      "com.vmunier" %% "play-scalajs-scripts" % "0.4.0",
      "org.monifu" %% "monifu" % monifuVer,

      "com.datastax.cassandra" % "cassandra-driver-core" % "3.0.0"
        exclude("org.xerial.snappy", "snappy-java")
        exclude("com.google.guava", "guava"),

      "org.scala-lang" % "scala-compiler" % scalaV,
      "org.scalatest" %% "scalatest" % scalatestV
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
      "org.scala-js" %%% "scalajs-dom" % "0.9.0",
      "be.doeraene" %%% "scalajs-jquery" % "0.9.0",
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

scalaJSUseRhino in Global := false // please install nodejs
