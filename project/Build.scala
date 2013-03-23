import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

  val appName         = "wenria-api-play2"
  val appVersion      = "1.0-SNAPSHOT"

  val appDependencies = Seq(
    "com.typesafe" % "play-slick_2.10" % "0.3.0",
    "net.coobird" % "thumbnailator" % "0.4.3",
    "com.github.scala-incubator.io" %% "scala-io-core" % "0.4.2",
    "com.github.scala-incubator.io" %% "scala-io-file" % "0.4.2",
    "com.github.tototoshi" %% "slick-joda-mapper" % "0.1.0",
    "mysql" % "mysql-connector-java" % "5.1.22"
  )

  val main = play.Project(appName, appVersion, appDependencies).settings(
    resolvers += Resolver.url("github repo for play-slick", url("http://loicdescotte.github.com/releases/"))(Resolver.ivyStylePatterns)
  )

}
