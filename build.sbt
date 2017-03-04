import sbt.Project.projectToRef

lazy val scalaV = "2.12.1"
lazy val monixV = "2.2.2"
lazy val scalatestV = "3.0.1"

lazy val server = (project in file("server"))
  .enablePlugins(PlayScala)
  .aggregate(clients.map(projectToRef): _*)
  .dependsOn(sharedJvm)
  .configs(UnitTest)
  .settings(inConfig(UnitTest)(Defaults.testTasks): _*)
  .settings(commonSettings ++ testSettings)
  .settings(
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
        exclude("com.google.guava", "guava")
        exclude("io.monix", "monix_2.11")
        exclude("io.monix", "monix"),
      "io.getquill" % "quill-cassandra_2.11" % "1.1.0"
        exclude("io.monix", "monix")
        exclude("io.monix", "monix_2.11"),
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
      "io.monix" %%% "monix" % monixV
    )
  )

lazy val clients = Seq(client)

lazy val shared = (crossProject.crossType(CrossType.Pure) in file("shared"))
  .settings(commonSettings: _*)
  .jsConfigure(_ enablePlugins ScalaJSPlay)

lazy val sharedJvm = shared.jvm
lazy val sharedJs = shared.js

lazy val commonSettings = Seq(scalaVersion := scalaV)
lazy val testSettings = Seq(
  libraryDependencies ++= Seq(
    "org.scalatest" %% "scalatest" % scalatestV % "test",
    "org.scalatestplus.play" %% "scalatestplus-play" % "2.0.0-M2" % "test",
    "org.scalamock" %% "scalamock-scalatest-support" % "3.5.0" % "test",
    "org.mockito" % "mockito-core" % "2.6.2" % Test,
    "com.storm-enroute" %% "scalameter-core" % "0.8.2" % "test"
  )
)
lazy val UnitTest = config("unit") extend Test

// loads the Play project at sbt startup
onLoad in Global := (Command.process("project server", _: State)) compose (onLoad in Global).value
