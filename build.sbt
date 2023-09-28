ThisBuild / organization := "br.etc.bruno"
ThisBuild / version := "0.1-SNAPSHOT"
ThisBuild / scalaVersion := scala2x

lazy val scala3x = "3.3.0"
lazy val scala2x = "2.13.12"

lazy val shared = (project in file("app-shared")).settings()

lazy val http4sVer = "0.23.23"
lazy val circeVer = "0.14.6"
lazy val `app-http4s` = (project in file("app-http4s"))
  .dependsOn(shared)
  .settings(
    scalacOptions ++= Seq(
      "-deprecation",
      "-feature",
      "-unchecked",
      "future"
    ),
    crossScalaVersions := Seq(scala2x, scala3x),
    libraryDependencies ++= Seq(
      //"org.typelevel" %% "cats-effect" % "3.5.0",
      "org.http4s" %% "http4s-dsl"          % http4sVer,
      "org.http4s" %% "http4s-circe"        % http4sVer,

      "io.circe" %% "circe-generic" % circeVer,

      "org.http4s" %% "http4s-ember-server" % http4sVer,
      //"org.http4s" %% "http4s-blaze-server" % "0.23.15",
      //"org.http4s" %% "http4s-netty-server" % "0.5.10",

      "org.postgresql" % "postgresql"        % "42.6.0",

      // "ch.qos.logback" % "logback-classic" % "1.4.11"
      "org.slf4j" % "slf4j-nop" % "2.0.9"
    ),
    assembly / mainClass := Some("etc.rinha.app.Http4sApp"),
    //assembly / assemblyJarName := s"${name.value}.jar",
    assembly / assemblyJarName := s"app.jar",
    assembly / assemblyMergeStrategy := {
      case "META-INF/io.netty.versions.properties" => MergeStrategy.first
      case "META-INF/versions/9/module-info.class" => MergeStrategy.last
      case x                                       =>
        val oldStrategy = (ThisBuild / assemblyMergeStrategy).value
        oldStrategy(x)
    }
  )

lazy val root = (project in file("."))
  .settings()