import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

  val appName         = "wenria-api-play2"
  val appVersion      = "1.0-SNAPSHOT"

  val appDependencies = Seq(
//    "com.typesafe" % "play-slick_2.10" % "0.3.0"
  )


  val main = play.Project(appName, appVersion, appDependencies).settings(
//    resolvers += Resolver.url("github repo for play-slick", url("http://loicdescotte.github.com/releases/"))(Resolver.ivyStylePatterns)
  ).dependsOn(RootProject( uri("git://github.com/freekh/play-slick.git") ))

}