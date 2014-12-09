Common.serviceSettings("manager")

// Add here the specific settings for this module


libraryDependencies ++= Common.commonDependencies ++: Seq(
  "org.scalatestplus" %% "play" % "1.2.0" % "test",
  "org.apache.tika" % "tika-parsers" % "1.4"
)
