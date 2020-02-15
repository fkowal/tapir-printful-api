ThisBuild / scalaVersion := "2.13.1"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "com.printful"
ThisBuild / organizationName := "api"

lazy val root = (project in file("."))
  .settings(
    name := "printful",
    libraryDependencies ++= List(
      "org.scalatest" %% "scalatest" % "3.0.8",
      "com.softwaremill.sttp.tapir" %% "tapir-core" % "0.12.20",
      "com.softwaremill.sttp.tapir" %% "tapir-openapi-docs" % "0.12.20",
      "com.softwaremill.sttp.tapir" %% "tapir-openapi-circe-yaml" % "0.12.20",
      "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-akka-http" % "0.12.20",
      "com.softwaremill.sttp.tapir" %% "tapir-json-circe" % "0.12.20",
      "com.softwaremill.sttp.tapir" %% "tapir-akka-http-server" % "0.12.20",
      "com.softwaremill.sttp.tapir" %% "tapir-sttp-client" % "0.12.20",
      "ch.qos.logback" % "logback-classic" % "1.2.3",
      "ch.qos.logback" % "logback-core" % "1.2.3",
      "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2",
      "com.softwaremill.sttp.client" %% "async-http-client-backend-fs2" % Versions.sttp % "test"
    )
  )
