import sbt.Project.projectToRef

lazy val scalaV = "2.11.8"
lazy val monixV = "2.1.2"
lazy val scalatestV = "2.2.6"

lazy val server = project
  .enablePlugins(PlayScala)
  .aggregate(clients.map(projectToRef): _*)
  .dependsOn(sharedJvm)
  .configs(UnitTest)
  .settings(
    inConfig(UnitTest)(Defaults.testTasks),
    commonSettings,
    testSettings,
    includeFilter in(Assets, LessKeys.less) := "*.less",
    name := "devgym",
    version := "1.0-SNAPSHOT",
    testOptions in UnitTest += Tests.Argument("-l", "RequireDB"),
    scalaJSProjects := clients,
    pipelineStages := Seq(scalaJSProd, gzip),
    libraryDependencies ++= Seq(
      cache,
      ws,
      "org.webjars" % "jquery" % "2.2.4",
      "org.webjars" % "bootstrap" % "3.3.6" exclude("org.webjars", "jquery"),
      "io.monix" %% "monix" % monixV,
      "com.datastax.cassandra" % "cassandra-driver-core" % "3.1.2"
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
    commonSettings,
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
  .settings(commonSettings)
  .jsConfigure(_ enablePlugins ScalaJSPlay)

lazy val sharedJvm = shared.jvm
lazy val sharedJs = shared.js

lazy val commonSettings = Seq(scalaVersion := scalaV)
lazy val testSettings = Seq(
  libraryDependencies ++= Seq(
    "org.scalatest" %% "scalatest" % scalatestV % Test,
    "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % Test,
    "org.scalamock" %% "scalamock-scalatest-support" % "3.2.2" % Test,
    "org.mockito" % "mockito-core" % "2.6.2" % Test,
    "com.storm-enroute" %% "scalameter-core" % "0.7" % Test
  )
)
lazy val UnitTest = config("unit") extend Test

// loads the Play project at sbt startup
onLoad in Global := (Command.process("project server", _: State)) compose (onLoad in Global).value
