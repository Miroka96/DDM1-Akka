import com.typesafe.sbt.SbtMultiJvm.multiJvmSettings
import com.typesafe.sbt.SbtMultiJvm.MultiJvmKeys.MultiJvm

organization := "one.codebase"

scalaVersion := "2.12.7"
val akkaVersion = "2.5.18"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-remote" % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster" % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster-metrics" % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster-tools" % akkaVersion,
  "com.typesafe.akka" %% "akka-multi-node-testkit" % akkaVersion,
  "org.scalatest" %% "scalatest" % "3.0.5" % Test,
  "io.kamon" % "sigar-loader" % "1.6.6-rev002")

scalacOptions in Compile ++= Seq("-deprecation", "-feature", "-unchecked", "-Xlog-reflective-calls", "-Xlint")
javacOptions in Compile ++= Seq("-Xlint:unchecked", "-Xlint:deprecation")
javaOptions in run ++= Seq("-Xms128m", "-Xmx1024m", "-Djava.library.path=./target/native")
fork in run := true
parallelExecution in Test := false


lazy val `skynet` = project
  .in(file("."))
  .settings(multiJvmSettings: _*)
  .settings(
    name := "skynet",
    version := "1.3.3.7",
    mainClass in (Compile, run) := Some("skynet.cluster.SimpleClusterApp")
  )
  .configs (MultiJvm)
