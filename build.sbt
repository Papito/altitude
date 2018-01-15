organization := "software.altitude"
name := "altitude"
version := "0.1.0-SNAPSHOT"
scalaVersion := "2.11.11"

val json4sversion = "3.5.3"
val ScalatraVersion = "2.6.2"
val jettyVersion = "9.4.7.v20170914"

libraryDependencies ++= Seq(
  "org.json4s"                  %% "json4s-jackson"      % "3.5.3",

  "org.scalatra"                %% "scalatra"            % ScalatraVersion,
  "org.scalatra"                %% "scalatra-scalate"    % ScalatraVersion,
  "org.scalatra"                %% "scalatra-specs2"     % ScalatraVersion % Test,
  "org.scalatra"                %% "scalatra-atmosphere" % ScalatraVersion,

  "com.typesafe.play"           %% "play-json"             % "2.3.10",

  "org.apache.tika"              % "tika-parsers"          % "1.14",
  "org.apache.tika"              % "tika-serialization"    % "1.14",

  "commons-io"                   % "commons-io"            % "2.5",
  "commons-dbutils"              % "commons-dbutils"       % "1.6",
  "org.postgresql"               % "postgresql"            % "9.4-1201-jdbc41",
  "org.xerial"                   % "sqlite-jdbc"           % "3.15.1",

  "net.codingwell"              %% "scala-guice"           % "4.1.0",
  "org.imgscalr"                 % "imgscalr-lib"          % "4.2",

  "ch.qos.logback"               % "logback-classic"       % "1.1.2" % "runtime",

  "org.eclipse.jetty"           %  "jetty-webapp"        % jettyVersion % "container;compile",
  "javax.servlet"               %  "javax.servlet-api"   % "3.1.0" % Provided
).map(_.exclude("commons-logging", "commons-logging"))
 .map(_.exclude("org.apache.cxf", "cxf-core"))
 .map(_.exclude("org.apache.cxf", "cxf-cxf-rt-transports-http"))

enablePlugins(ScalatraPlugin)

test in assembly := {}

assemblyMergeStrategy in assembly := {
  case x if x.startsWith("META-INF") => MergeStrategy.discard
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}
