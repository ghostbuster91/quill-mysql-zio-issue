import sbt._
import sbt.Keys._

version := versions.app
scalaVersion := versions.scala
name := "quill-issue"

val scalacCustomOptions = Seq(
  "-encoding",
  "UTF-8",
  "-opt-warnings",
  "-feature",
  "-deprecation",
  "-encoding",
  "UTF-8",
  "-unchecked",
  "-deprecation",
  "-Xlint",
  "-Ywarn-dead-code",
  "-Ywarn-extra-implicit",
  "-Ywarn-numeric-widen",
  "-Ymacro-annotations",
  "-language:postfixOps",
  "-language:higherKinds",
  "-Ywarn-value-discard",
  "-Wunused",
  "-Wconf:cat=lint-package-object-classes:s,cat=lint-byname-implicit:s",
  "-Ymacro-annotations"
)

lazy val zio = Seq(
  "dev.zio"              %% "zio"                     % versions.zio,
  "dev.zio"              %% "zio-macros"              % versions.zio,
  "dev.zio"              %% "zio-config"              % versions.`zio-config`,
  "dev.zio"              %% "zio-config-typesafe"     % versions.`zio-config`,
  "dev.zio"              %% "zio-config-gen"          % versions.`zio-config`,
  "dev.zio"              %% "zio-json"                % versions.`zio-json`,
  "dev.zio"              %% "zio-json-interop-http4s" % versions.`zio-json`,
  "dev.zio"              %% "zio-logging"             % versions.`zio-logging`,
  "dev.zio"              %% "zio-logging-slf4j"       % versions.`zio-logging`,
  "io.github.kitlangton" %% "zio-magic"               % versions.`zio-magic`,
  "dev.zio"              %% "zio-prelude"             % "1.0.0-RC6"
)

lazy val logging = Seq(
  "ch.qos.logback"       % "logback-classic"          % "1.2.3",
  "ch.qos.logback"       % "logback-core"             % "1.2.3",
  "net.logstash.logback" % "logstash-logback-encoder" % "6.4"
)

lazy val data = Seq(
  "mysql"         % "mysql-connector-java" % versions.`mysql-connector`,
  "io.getquill"   %% "quill-jdbc-zio" % versions.quill,
  "org.flywaydb"   % "flyway-core"    % versions.flyway,
)


lazy val tests = Seq(
  "dev.zio"      %% "zio-test"                   % versions.zio % "test",
  "dev.zio"      %% "zio-test-sbt"               % versions.zio % "test"
)

lazy val commonSettings = Seq(
  version := versions.app,
  scalaVersion := versions.scala
)

lazy val app = project
  .in(file("."))
  .settings(commonSettings)
  .settings(
    scalacOptions ++= scalacCustomOptions,
    libraryDependencies ++= zio ++ logging ++ data ++ tests,
    testFrameworks := Seq(new TestFramework("zio.test.sbt.ZTestFramework"))
  )

Compile / unmanagedClasspath ++= Seq(
  sourceDirectory.value / "main" / "resources"
)

run / fork := true
run / javaOptions ++= Seq("-Duser.timezone=UTC", "-Dkryo.unsafe=false")
Test / fork := true

addCompilerPlugin(("org.typelevel" % "kind-projector" % "0.13.0").cross(CrossVersion.full))

