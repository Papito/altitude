organization := "software.altitude"
name := "altitude"
version := "0.1.0-SNAPSHOT"
scalaVersion := "2.11.12"

val json4sversion = "3.5.3"
val ScalatraVersion = "2.6.2"
val jettyVersion = "9.4.20.v20190813"

libraryDependencies ++= Seq(
  "org.json4s"                  %% "json4s-jackson"        % "3.5.3",

  "org.scalatra"                %% "scalatra"              % ScalatraVersion,
  "org.scalatra"                %% "scalatra-atmosphere"   % ScalatraVersion,
  "org.scalatra"                %% "scalatra-scalatest"    % ScalatraVersion % Test,
  "org.mockito"                  % "mockito-core"          % "2.23.0" % Test,

  "com.typesafe.play"           %% "play-json"             % "2.6.8",

  "org.apache.tika"              % "tika-parsers"          % "1.14",
  "org.apache.tika"              % "tika-serialization"    % "1.14",

  "commons-io"                   % "commons-io"            % "2.5",
  "commons-dbutils"              % "commons-dbutils"       % "1.6",
  "org.postgresql"               % "postgresql"            % "9.4-1201-jdbc41",
  "org.xerial"                   % "sqlite-jdbc"           % "3.15.1",

  "com.google.guava"             % "guava"                 % "19.0",
  "net.codingwell"              %% "scala-guice"           % "4.1.1",
  "org.imgscalr"                 % "imgscalr-lib"          % "4.2",

  "ch.qos.logback"               % "logback-classic"       % "1.1.2" % "runtime",

  "org.eclipse.jetty"            % "jetty-webapp"          % jettyVersion % "container;compile",
  "javax.servlet"                % "javax.servlet-api"     % "3.1.0" % Provided
).map(_.exclude("commons-logging", "commons-logging"))
 .map(_.exclude("org.apache.cxf", "cxf-core"))
 .map(_.exclude("org.apache.cxf", "cxf-cxf-rt-transports-http"))

enablePlugins(ScalatraPlugin)

test in assembly := {}

parallelExecution in Test := false

assemblyMergeStrategy in assembly := {
  case x if x.startsWith("META-INF") => MergeStrategy.discard
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}

commands += Command.command("testFocused") { state =>
  "testOnly -- -n focused" :: state
}
commands += Command.command("testFocusedSqlite") { state =>
  "testOnly software.altitude.test.core.suites.SqliteSuite -- -n focused" :: state
}
commands += Command.command("testFocusedPostgres") { state =>
  "testOnly software.altitude.test.core.suites.PostgresSuite -- -n focused" :: state
}
commands += Command.command("testSqlite") { state =>
  "testOnly software.altitude.test.core.suites.SqliteSuite" :: state
}
commands += Command.command("testPostgres") { state =>
  "testOnly software.altitude.test.core.suites.PostgresSuite" :: state
}
commands += Command.command("watch") { state =>
  "~;jetty:stop;jetty:start" :: state
}

javaOptions ++= Seq( "-Xdebug", "-Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005" )
