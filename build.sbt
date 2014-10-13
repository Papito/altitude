Common.appSettings


lazy val common = (project in file("modules/common")).enablePlugins(PlayScala)

lazy val manager = (project in file("modules/manager")).enablePlugins(PlayScala).dependsOn(common)

lazy val client = (project in file("modules/client")).enablePlugins(PlayScala).dependsOn(common)

lazy val root = (project in file(".")).enablePlugins(PlayScala).aggregate(common, manager, client).dependsOn(common,
manager, client)


libraryDependencies ++= Common.commonDependencies
