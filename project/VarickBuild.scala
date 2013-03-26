import sbt._
import Keys._

object VarickBuild extends Build {

  lazy val scalatestLib =  "org.scalatest" % "scalatest_2.10" % "1.9.1" % "test"

  lazy val buildSettings = Project.defaultSettings ++  Seq(
    version := "ALPHA", 
    scalaVersion :=  "2.10.1",
    parallelExecution in Test := false,
    scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature", "-language:postfixOps"),
    libraryDependencies ++= Seq(scalatestLib)
  )

  lazy val core = Project(id = "varick-core", 
                          base = file("varick-core"),
                          settings = buildSettings
                        )//.dependsOn(event)

  lazy val event = Project(id = "varick-event", 
    base = file("varick-event"),
    settings = buildSettings)
}
