import play.PlayImport._
import sbt.Keys._
import sbt._

object Common {
	def appName = "altitude"
	
	// Common settings for every project
	def settings (theName: String) = Seq(
		name := theName,
		organization := "altitude",
		version := "1.0-SNAPSHOT",
		scalaVersion := "2.11.1",
		doc in Compile <<= target.map(_ / "none"),
		scalacOptions ++= Seq("-feature", "-deprecation", "-unchecked", "-language:reflectiveCalls")
	)
	// Settings for the app, i.e. the root project
	val appSettings = settings(appName)
	// Settings for every module, i.e. for every subproject
	def moduleSettings (module: String) = settings(module) ++: Seq(
		javaOptions in Test += s"-Dconfig.resource=application.conf"
	)
	// Settings for every service, i.e. for manager and client subprojects
	def serviceSettings (module: String) = moduleSettings(module) ++: Seq(
	)
	
	val commonDependencies = Seq(
		cache
	)
}
