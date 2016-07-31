package service.sbt

import java.nio.charset.StandardCharsets
import java.nio.file._
import java.nio.file.attribute.BasicFileAttributes

import scala.sys.process.{Process, ProcessLogger}
import scala.util.Try

object SbtTestRunner {

  def createProjectAndTest(solution: String, appAbsolutePath: String): String = {
    import scala.collection.JavaConversions._
    Try {
      val buildSbt = List(
        """lazy val root = (project in file("."))""",
        """  .settings(""",
        """    scalaVersion := "2.11.7",""",
        """    libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.6" % "test"""",
        """  )"""
      )
      val d = Files.createTempDirectory("test")
      Files.write(d.resolve("build.sbt"), buildSbt, StandardCharsets.UTF_8)

      val project = d.resolve("project")
      Files.createDirectory(project)

      Files.write(project.resolve("build.properties"), List("sbt.version=0.13.9"), StandardCharsets.UTF_8)

      val solutionTargetPath = d.resolve("src").resolve("main").resolve("scala")
      Files.createDirectories(solutionTargetPath)
      Files.write(solutionTargetPath.resolve("UserSolution.scala"), List("package tests", s"object SleepInSolution {$solution}"), StandardCharsets.UTF_8)

      val testTargetPath = d.resolve("src").resolve("test").resolve("scala")
      Files.createDirectories(testTargetPath)
      val testSourcePath = Paths.get(appAbsolutePath, "test", "tests")

      Files.walkFileTree(testSourcePath, new SimpleFileVisitor[Path] {
        override def visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult = {
          Files.copy(file, testTargetPath.resolve(testSourcePath.relativize(file)))
          FileVisitResult.CONTINUE
        }
      })
      val sbtCommands = Seq("sbt", "-Dsbt.log.noformat=true", "test")
      val output = new StringBuilder
      Process(sbtCommands, d.toFile).!(ProcessLogger(line => output append line append "\n"))
      output.toString()
      //TODO: we need to clean all temp folders
    }.getOrElse("Test failed")
  }
}
