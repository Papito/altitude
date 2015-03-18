import play.PlayImport._
import play.PlayScala

name := "altitude"

version := "1.0-SNAPSHOT"

scalaVersion := "2.11.1"

doc in Compile <<= target.map(_ / "none")

scalacOptions ++= Seq("-feature", "-deprecation", "-unchecked", "-language:reflectiveCalls")

javaOptions in Test += s"-Dconfig.resource=application.test.conf"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

// Add here the specific settings for this module
libraryDependencies ++= Seq(
  "org.scalatestplus" % "play_2.11" % "1.2.0",
  "org.apache.tika" % "tika-parsers" % "1.4",
  "org.reactivemongo" % "play2-reactivemongo_2.11" % "0.10.5.0.akka23",
  "org.json4s" %% "json4s-native" % "3.2.11",
  "org.apache.commons" % "commons-io" % "1.3.2",
  "net.codingwell" %% "scala-guice" % "4.0.0-beta5",
  "commons-dbutils" % "commons-dbutils" % "1.6",
  cache,
  jdbc
)