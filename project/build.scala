import com.mojolly.scalate.ScalatePlugin.ScalateKeys._
import com.mojolly.scalate.ScalatePlugin._
import org.scalatra.sbt._
import sbt.Keys._
import sbt._

object AltitudeBuild extends Build {
  val Organization = "altitude"
  val Name = "Altitude"
  val Version = "0.1.0-SNAPSHOT"
  val ScalaVersion = "2.11.6"
  val ScalatraVersion = "2.4.0.RC1"

  lazy val project = Project (
    "altitude",
    file("."),
    settings = ScalatraPlugin.scalatraWithJRebel ++ scalateSettings ++ Seq(
      organization := Organization,
      name := Name,
      version := Version,
      scalaVersion := ScalaVersion,
      resolvers += Classpaths.typesafeReleases,
      resolvers += "Scalaz Bintray Repo" at "http://dl.bintray.com/scalaz/releases",
      resolvers += "Sonatype OSS Snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/",
      resolvers += "Akka Repo" at "http://repo.akka.io/repository",
      libraryDependencies ++= Seq(
        "org.scalatra" %% "scalatra" % ScalatraVersion,
        "org.scalatra" %% "scalatra-scalate" % ScalatraVersion,
        "org.scalatra" %% "scalatra-specs2" % ScalatraVersion % "test",
        "org.scalatra" %% "scalatra-atmosphere" % ScalatraVersion,
        "org.scalatra" %% "scalatra-json" % ScalatraVersion,

        "org.scalatra" %% "scalatra-scalatest" % "2.4.0.RC1" % "test",
        "org.scalatest" % "scalatest_2.11" % "2.2.4" % "test",

        "org.json4s" %% "json4s-native" % "3.2.11",
        "org.json4s" %% "json4s-jackson" % "3.2.10",
        "org.json4s" %% "json4s-mongo" % "3.2.10",

        "org.apache.tika" % "tika-parsers" % "1.7",
        "org.apache.tika" % "tika-serialization" % "1.7",
        "org.apache.commons" % "commons-io" % "1.3.2",

        "ch.qos.logback" % "logback-classic" % "1.1.2" % "runtime",
        "net.codingwell" %% "scala-guice" % "4.0.0-beta5",
        "commons-dbutils" % "commons-dbutils" % "1.6",
        "org.imgscalr" % "imgscalr-lib" % "4.2",
        "org.postgresql" % "postgresql" % "9.4-1201-jdbc41",
        "com.typesafe.play" %% "play-json" % "2.3.4",
        "org.mongodb" % "casbah_2.11" % "2.8.1",
        "org.mongodb" % "casbah-commons_2.11" % "2.8.1",

        "org.eclipse.jetty" % "jetty-websocket" % "8.1.17.v20150415" % "container",
        "org.eclipse.jetty" % "jetty-webapp" % "9.2.11.v20150529" % "container",
        "org.eclipse.jetty" % "jetty-plus" % "9.2.11.v20150529" % "container",
        "javax.servlet" % "javax.servlet-api" % "3.1.0"
      ),
      scalateTemplateConfig in Compile <<= (sourceDirectory in Compile){ base =>
        Seq(
          TemplateConfig(
            base / "webapp" / "WEB-INF" / "templates",
            Seq.empty,  /* default imports should be added here */
            Seq(
              Binding("context", "_root_.org.scalatra.scalate.ScalatraRenderContext", importMembers = true, isImplicit = true)
            ),  /* add extra bindings here */
            Some("templates")
          )
        )
      }
    )
  )
}
