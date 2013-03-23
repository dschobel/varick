import sbt._
import Keys._

object VarickBuild extends Build {

  lazy val scalatestLib =  "org.scalatest" % "scalatest_2.10.0" % "2.0.M5" % "test"

  lazy val buildSettings = Project.defaultSettings ++  Seq(
    version := "ALPHA", 
    scalaVersion :=  "2.10.1",
    scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature"),
    libraryDependencies ++= Seq(scalatestLib)
  )

  lazy val core = Project(id = "varick-core", 
                          base = file("varick-core"),
                          settings = buildSettings
                        ).dependsOn(event)

  lazy val event = Project(id = "varick-event", 
    base = file("varick-event"),
    settings = buildSettings)
}
