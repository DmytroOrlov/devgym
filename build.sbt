import sbt.Project.projectToRef

lazy val scalaV = "2.12.1"
lazy val monixV = "2.2.1"
lazy val scalatestV = "3.0.1"

lazy val server = project
  .enablePlugins(PlayScala)
  .aggregate(clients.map(projectToRef): _*)
  .dependsOn(sharedJvm)
  .configs(UnitTest)
  .settings(
    inThisBuild(Seq(
      version := "1.0-SNAPSHOT",
      scalaVersion := scalaV
    )),
    inConfig(UnitTest)(Defaults.testTasks),
    testSettings,
    includeFilter in(Assets, LessKeys.less) := "*.less",
    name := "devgym",
    testOptions in UnitTest += Tests.Argument("-l", "RequireDB"),
    scalaJSProjects := clients,
    pipelineStages := Seq(scalaJSProd, gzip),
    libraryDependencies ++= Seq(
      cache,
      ws,
      "org.webjars" % "jquery" % "2.2.4",
      "org.webjars" % "bootstrap" % "3.3.6" exclude("org.webjars", "jquery"),
      "io.monix" %% "monix" % monixV,
      "com.datastax.cassandra" % "cassandra-driver-core" % "3.1.3"
        exclude("org.xerial.snappy", "snappy-java")
        exclude("com.google.guava", "guava"),
      "io.getquill" %% "quill-cassandra" % "1.0.1",
      "org.scala-lang" % "scala-compiler" % scalaV,
      "org.scalatest" %% "scalatest" % scalatestV,
      "org.scalameta" %% "scalameta" % "1.4.0"
    )
  )

lazy val client = project
  .enablePlugins(ScalaJSPlugin, ScalaJSPlay)
  .dependsOn(sharedJs)
  .settings(
    persistLauncher := false,
    persistLauncher in Test := false,
    libraryDependencies ++= Seq(
      "org.scala-js" %%% "scalajs-dom" % "0.9.1",
      "be.doeraene" %%% "scalajs-jquery" % "0.9.1",
      "io.monix" %%% "monix" % monixV
    )
  )

lazy val clients = Seq(client)

lazy val shared = crossProject.crossType(CrossType.Pure)
  .jsConfigure(_ enablePlugins ScalaJSPlay)

lazy val sharedJvm = shared.jvm
lazy val sharedJs = shared.js

lazy val testSettings = Seq(
  libraryDependencies ++= Seq(
    "org.scalatest" %% "scalatest" % scalatestV % Test,
    "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % Test,
    "org.scalamock" %% "scalamock-scalatest-support" % "3.2.2" % Test,
    "org.mockito" % "mockito-core" % "2.6.9" % Test,
    "com.storm-enroute" %% "scalameter-core" % "0.8.2" % Test
  )
)
lazy val UnitTest = config("unit") extend Test

// loads the Play project at sbt startup
onLoad in Global := (Command.process("project server", _: State)) compose (onLoad in Global).value
