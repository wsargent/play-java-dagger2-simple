name := """play-java-dagger2-simple"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayJava)

scalaVersion := "2.11.8"

libraryDependencies += "com.google.dagger" % "dagger" % "2.7"
libraryDependencies += "com.google.dagger" % "dagger-compiler" % "2.7"
