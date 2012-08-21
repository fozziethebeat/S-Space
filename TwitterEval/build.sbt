import AssemblyKeys._ // put this at the top of the file

name := "TwitterEval"

version := "1.0.0"

organization := "edu.ucla.sspace"

resolvers += "repo.codahale.com" at "http://repo.codahale.com"

resolvers += "Local Maven Repository" at "file://"+Path.userHome.absolutePath+"/.m2/repository"

scalaVersion := "2.9.2"

  //"edu.stanford.nlp" % "stanford-corenlp" % "1.3.3",
  //"edu.ucla.sspace" %% "scalda" % "0.0.1",
libraryDependencies ++= Seq(
  "com.codahale" % "jerkson_2.9.1" % "0.5.0",
  "org.scalanlp" %% "breeze-math" % "0.1-SNAPSHOT",
  "org.scalanlp" %% "breeze-learn" % "0.1-SNAPSHOT",
  "org.apache.opennlp" % "opennlp-tools" % "1.5.2-incubating",
  "org.ccil.cowan.tagsoup" % "tagsoup" % "1.2.1",
  "edu.ucla.sspace" % "sspace-wordsi" % "2.0.3"
)

scalacOptions += "-deprecation"

assemblySettings
