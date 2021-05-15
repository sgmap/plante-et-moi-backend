name := """plante-et-moi-backend"""
organization := "fr.gouv.beta"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.12.8"

//libraryDependencies += filters

libraryDependencies ++= Seq(
  ws,
  jdbc,
  evolutions,
  guice
)

libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.2" % Test

libraryDependencies ++= Seq(
  "org.webjars" %% "webjars-play" % "2.6.3",
  "org.webjars.bower" % "material-design-lite" % "1.3.0",
  "org.webjars" % "material-design-icons" % "3.0.1",
  "org.webjars.npm" % "roboto-fontface" % "0.9.0",
  "org.webjars.npm" % "leaflet" % "1.0.2",
  "org.postgresql" % "postgresql" % "42.2.2",
  "org.playframework.anorm" %% "anorm" % "2.6.2",
  "com.typesafe.play" %% "play-mailer" % "7.0.0" ,
  "org.playframework.anorm" %% "anorm" % "2.6.2",
  "com.typesafe.play" %% "play-json-joda" % "2.7.3",
  "com.typesafe.play" %% "play-mailer-guice" % "7.0.0",
  "com.github.tototoshi" %% "scala-csv" % "1.3.8"
)


// Crash
libraryDependencies += "io.sentry" % "sentry-logback" % "1.7.5"

// Adds additional packages into Twirl
//TwirlKeys.templateImports += "fr.gouv.beta.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "fr.gouv.beta.binders._"
