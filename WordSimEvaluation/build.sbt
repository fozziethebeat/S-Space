
name := "WordSimExperiment"

version := "0.1"

scalaVersion := "2.9.2"

resolvers += "Local Maven Repository" at "file://"+Path.userHome.absolutePath+"/.m2/repository"

libraryDependencies ++= Seq(
    "edu.ucla.sspace" % "sspace-wordsi" % "2.0",
    "cc.mallet" % "mallet" % "2.0.7"
)

scalacOptions += "-deprecation"
