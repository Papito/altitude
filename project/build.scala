import sbt._
import sbt.Keys._

import org.scalatra.sbt._

import com.mojolly.scalate._
import com.mojolly.scalate.ScalatePlugin._
import com.mojolly.scalate.ScalatePlugin.ScalateKeys._
import sbtassembly.AssemblyPlugin._
import sbtassembly.AssemblyKeys._

object AltitudeBuild extends Build {
  val Organization = "altitude"
  val Name = "Altitude"
  val Version = "0.1.0-SNAPSHOT"
  val ScalaVersion = "2.11.6"
  val ScalatraVersion = "2.3.0"
  val json4sversion = "3.2.9"
  val jettyVersion = "9.1.3.v20140225"

  val projectSettings = ScalatraPlugin.scalatraSettings ++ Seq(
    organization := Organization,
    name := Name,
    parallelExecution in Test := false,
    version := Version,
    scalaVersion := ScalaVersion,
    resolvers += Classpaths.typesafeReleases,
    test in assembly := {},
    resolvers += "Typesafe" at "http://dl.bintray.com/typesafe/maven-releases",
    resolvers += "Scalaz Bintray Repo" at "http://dl.bintray.com/scalaz/releases",
    resolvers += "Sonatype OSS Snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/",
    resolvers += "Akka Repo" at "http://repo.akka.io/repository",
    libraryDependencies ++= Seq(
      "org.scalatra"                %% "scalatra"              % ScalatraVersion,
      "org.scalatra"                %% "scalatra-scalate"      % ScalatraVersion,
      "org.scalatra"                %% "scalatra-atmosphere"   % ScalatraVersion,
      "org.scalatra"                %% "scalatra-scalatest"    % ScalatraVersion % "test",
      "org.json4s"                  %% "json4s-jackson"        % json4sversion,
      "org.json4s"                  %% "json4s-mongo"          % json4sversion,
      "com.typesafe.play"           %% "play-json"             % "2.3.10",


      "org.apache.tika"              % "tika-parsers"          % "1.7",
      "org.apache.tika"              % "tika-serialization"    % "1.7",
      "commons-io"                   % "commons-io"            % "2.4",
      "commons-dbutils"              % "commons-dbutils"       % "1.6",

      "ch.qos.logback"               % "logback-classic"       % "1.1.2" % "runtime",
      "net.codingwell"              %% "scala-guice"           % "4.0.0-beta5",
      "org.imgscalr"                 % "imgscalr-lib"          % "4.2",

      "org.postgresql"               % "postgresql"            % "9.4-1201-jdbc41",
      "org.mongodb"                  % "casbah_2.11"           % "2.8.1",
      "org.mongodb"                  % "casbah-commons_2.11"   % "2.8.1",
      "org.xerial"                   % "sqlite-jdbc"           % "3.8.11.1",

      "org.eclipse.jetty"            %  "jetty-plus"           % jettyVersion % "container;compile",
      "org.eclipse.jetty.websocket"  %  "websocket-server"     % jettyVersion % "container;compile",
      "org.eclipse.jetty"            %  "jetty-webapp"         % jettyVersion % "container;compile",
      "javax.servlet"                %  "javax.servlet-api"    % "3.1.0" % "provided"
    )
  )

  lazy val project = Project("altitude", file("."))
    .settings(projectSettings: _*)
    .settings(ScalatePlugin.scalateSettings: _*)
    .settings(assemblySettings:_*)
}
