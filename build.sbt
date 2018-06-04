import sbt.Keys._
import sbt._
import sbtrelease.Version

name := "daytrader-rsi2"

version := "0.0.1-SNAPSHOT"

organization := "ai.daytrader"

resolvers += Resolver.sonatypeRepo("public")
scalaVersion := "2.11.11"
releaseNextVersion := { ver => Version(ver).map(_.bumpMinor.string).getOrElse("Error") }
assemblyJarName in assembly := "daytrader-rsi2.jar"

val akkaV = "2.5.3"

libraryDependencies ++= Seq(
  "com.typesafe.akka"       %% "akka-actor"                 % akkaV,
  "com.typesafe.akka"       %% "akka-stream"                % akkaV,
  "com.typesafe.akka"       %% "akka-http"                  % "10.0.9",
  "io.surfkit" %% "war-pony" % "0.0.1-SNAPSHOT",
  "com.amazonaws" % "aws-lambda-java-events" % "1.1.0",
  "com.amazonaws" % "aws-lambda-java-core" % "1.1.0",
  "jp.co.bizreach" %% "aws-dynamodb-scala" % "0.0.7"
)

scalacOptions ++= Seq(
  "-unchecked",
  "-deprecation",
  "-feature",
  "-Xfatal-warnings")
