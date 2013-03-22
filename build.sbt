//import com.typesafe.startscript.StartScriptPlugin

//seq(StartScriptPlugin.startScriptForClassesSettings: _*)

seq(com.github.retronym.SbtOneJar.oneJarSettings: _*)

name := "Varick"

version := "ALPHA"

scalaVersion := "2.10.1"

//resolvers += "twitter-repo" at "http://maven.twttr.com"

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

scalacOptions ++= Seq( "-deprecation", "-feature", "-language:postfixOps")

//autoCompilerPlugins := true

libraryDependencies += "org.scalatest" % "scalatest_2.10" % "1.9.1" % "test"

//libraryDependencies <+= scalaVersion {
 //     v => compilerPlugin("org.scala-lang.plugins" % "continuations" % "2.10.1")
//}
