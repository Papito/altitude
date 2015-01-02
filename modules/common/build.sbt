Common.moduleSettings("common")

libraryDependencies ++= Seq(
  "org.reactivemongo" % "play2-reactivemongo_2.11" % "0.10.5.0.akka23",
  "org.scalatestplus" % "play_2.11" % "1.2.0",
  "org.json4s" %% "json4s-native" % "3.2.11",
  "org.apache.commons" % "commons-io" % "1.3.2",
  "net.codingwell" %% "scala-guice" % "4.0.0-beta5"
)