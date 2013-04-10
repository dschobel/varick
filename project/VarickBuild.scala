import sbt._
import Keys._

object VarickBuild extends Build {

  lazy val http_coreLib =  "org.scalatest" % "scalatest_2.10" % "1.9.1" % "test"
  lazy val scalatestLib =  "org.apache.httpcomponents" % "httpcore-nio" % "4.2.3"


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
                        )

  lazy val event = Project(id = "varick-event", 
    base = file("varick-event"),
    settings = buildSettings)

  lazy val http = Project(id = "varick-http", 
    base = file("varick-http"),
    settings = buildSettings).dependsOn(core)

  lazy val examples = Project(id = "varick-examples", 
    base = file("varick-examples"),
    settings = buildSettings).dependsOn(http)
}
