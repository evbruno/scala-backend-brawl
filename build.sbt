ThisBuild / organization  := "br.etc.bruno"
ThisBuild / version       := "0.1-SNAPSHOT"

ThisBuild / scalaVersion  := "2.13.12"

ThisBuild / libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.8.0",
  "com.typesafe.akka" %% "akka-stream" % "2.8.0",
  "com.typesafe.akka" %% "akka-http" % "10.5.0"
)

ThisBuild / assemblyMergeStrategy := {
  case "reference.conf" => MergeStrategy.concat
  case x                =>
    val oldStrategy = (ThisBuild / assemblyMergeStrategy).value
    oldStrategy(x)
}

lazy val root = (project in file("."))
  .settings(
    assembly / assemblyJarName := s"app-${version.value}.jar",
  )