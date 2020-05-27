val settings: Seq[Def.Setting[String]] = Seq(
  name := "mirea-crypto",
  version := "0.1",
  scalaVersion := "2.12.10"
)

resolvers ++= Seq(
  "Sonatype Public" at "https://oss.sonatype.org/content/groups/public/",
  "Sonatype Releases" at "https://oss.sonatype.org/content/repositories/releases/",
  "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/",
  "Typesafe maven releases" at "https://repo.typesafe.com/typesafe/maven-releases/"
)

libraryDependencies ++= Seq(
  "co.fs2" %% "fs2-io" % "2.1.0",
  "eu.timepit" %% "refined"  % "0.9.14",
  "org.bouncycastle" % "bcprov-jdk15on" % "1.65",
  "org.scodec" %% "scodec-core" % "1.10.3",
  "io.chrisdavenport" %% "log4cats-slf4j" % "0.4.0-M2",
  "com.comcast" %% "ip4s-cats" % "1.2.1",
  "ru.tinkoff" %% "tofu" % "0.7.5"
)

addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)

val crypto = (project in file(".")).settings(settings: _*)