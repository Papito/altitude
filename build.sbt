Common.appSettings


lazy val common = (project in file("modules/common")).enablePlugins(PlayScala)

lazy val manager = (project in file("modules/manager")).enablePlugins(PlayScala).dependsOn(common)

lazy val web = (project in file("modules/web")).enablePlugins(PlayScala).dependsOn(common)

lazy val root = (project in file(".")).enablePlugins(PlayScala).aggregate(common, manager, web).dependsOn(common,
manager, web)


libraryDependencies ++= Common.commonDependencies
