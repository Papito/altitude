import sbt._

object AltitudeBuild extends Build {
  val Organization = "altitude"
  val Name = "Altitude"
  val Version = "0.1.0-SNAPSHOT"
  val ScalaVersion = "2.11.8"
  val ScalatraVersion = "2.4.0"
  val json4sversion = "3.3.0"
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
      "org.scalatra"                %% "scalatra-atmosphere"   % ScalatraVersion,
      "org.scalatra"                %% "scalatra-scalatest"    % ScalatraVersion % "test",
      "org.json4s"                  %% "json4s-jackson"        % json4sversion,
      "org.json4s"                  %% "json4s-mongo"          % json4sversion,
      "com.typesafe.play"           %% "play-json"             % "2.3.10",

      "org.apache.tika"              % "tika-parsers"          % "1.14",
      "org.apache.tika"              % "tika-serialization"    % "1.14",
      "commons-io"                   % "commons-io"            % "2.5",
      "commons-dbutils"              % "commons-dbutils"       % "1.6",

      "ch.qos.logback"               % "logback-classic"       % "1.1.2" % "runtime",
      "net.codingwell"              %% "scala-guice"           % "4.1.0",
      "org.imgscalr"                 % "imgscalr-lib"          % "4.2",

      "org.postgresql"               % "postgresql"            % "9.4-1201-jdbc41",
      "org.xerial"                   % "sqlite-jdbc"           % "3.15.1",

      "org.eclipse.jetty"            %  "jetty-plus"           % jettyVersion % "container;compile",
      "org.eclipse.jetty.websocket"  %  "websocket-server"     % jettyVersion % "container;compile",
      "org.eclipse.jetty"            %  "jetty-webapp"         % jettyVersion % "container;compile",
      "javax.servlet"                %  "javax.servlet-api"    % "3.1.0" % "provided"
    ).map(_.exclude("commons-logging", "commons-logging"))
     .map(_.exclude("org.apache.cxf", "cxf-core"))
     .map(_.exclude("org.apache.cxf", "cxf-cxf-rt-transports-http"))
  )

  assemblyMergeStrategy in assembly := {
    case x if x.startsWith("META-INF") => MergeStrategy.discard
    case x =>
      val oldStrategy = (assemblyMergeStrategy in assembly).value
      oldStrategy(x)
  }

  // settings for sbt-assembly plugin
  val deployAssemblySettings = assemblySettings ++ Seq(
    // copy web resources to /webapp folder
    resourceGenerators in Compile <+= (resourceManaged, baseDirectory) map {
      (managedBase, base) =>
        val webappBase = base / "src" / "main" / "webapp"
        for {
          (from, to) <- webappBase ** "*" pair rebase(webappBase, managedBase / "main" / "webapp")
        } yield {
          Sync.copy(from, to)
          to
        }
    },
    test in assembly := {}
  )

  lazy val project = Project("altitude", file("."))
    .settings(projectSettings: _*)
    .settings(scalateSettings: _*)
    .settings(deployAssemblySettings:_*)
}
