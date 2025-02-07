// openapi-diff-plugin/build.sbt
sbtPlugin := true

organization := "org.lxol"
name         := "openapi-diff-plugin"
version      := "0.1.4"
scalaVersion := "2.12.20" // Use a Scala 2.12 version since sbt 1.x uses Scala 2.12

libraryDependencies ++= Seq(
  "org.openapitools.openapidiff" % "openapi-diff-core" % "2.1.0-beta.12",
  "io.swagger.parser.v3"         % "swagger-parser"    % "2.1.25"
)
