Common.moduleSettings("common")

libraryDependencies ++= Seq(
  "org.eu.acolyte" % "reactive-mongo_2.11" % "1.0.31",
  "org.scalatestplus" %% "play" % "1.2.0" % "test",
  "org.json4s" %% "json4s-native" % "3.2.11",
  "org.apache.commons" % "commons-io" % "1.3.2"
)