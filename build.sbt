import AssemblyKeys._ // put this at the top of the file

name := "FScape"

version := "1.0.0"

organization := "de.sciss"

description := "A standalone audio rendering software for time domain and spectral signal processing"

homepage <<= name { n => Some(url("https://github.com/Sciss/" + n)) }

licenses := Seq("GPL v2+" -> url("http://www.gnu.org/licenses/gpl-2.0.txt"))

scalaVersion := "2.10.2"

crossPaths := false  // this is just a Java project right now!

autoScalaLibrary := false

retrieveManaged := true

mainClass in Compile := Some("de.sciss.fscape.Main")

// libraryDependencies ++= Seq(
//    "de.sciss" %% "submin" % "0.10-SNAPSHOT"
// )

// ---- bundling ----

seq(assemblySettings: _*)

test in assembly := {}

target in assembly <<= baseDirectory

jarName in assembly <<= (name, version) map { _ + "-" + _ + ".jar" }

seq(appbundle.settings: _*)

appbundle.javaOptions ++= Seq("-Xmx1024m", "-ea")

appbundle.icon := Some(file("icons") / "application.icns")

appbundle.target <<= baseDirectory

appbundle.mainClass <<= mainClass in Compile

appbundle.signature := "FSc "

appbundle.documents += appbundle.Document(
  name       = "FScape Document",
  role       = appbundle.Document.Editor,
  icon       = Some(file("icons") / "document.icns"),
  extensions = Seq("fsc")
)

// ---- publishing ----

publishMavenStyle := true

publishTo <<= version { v =>
  Some(if (v endsWith "-SNAPSHOT")
    "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
  else
    "Sonatype Releases"  at "https://oss.sonatype.org/service/local/staging/deploy/maven2"
  )
}

publishArtifact in Test := false

pomIncludeRepository := { _ => false }

pomExtra <<= name { n =>
<scm>
  <url>git@github.com:Sciss/{n}.git</url>
  <connection>scm:git:git@github.com:Sciss/{n}.git</connection>
</scm>
<developers>
  <developer>
    <id>sciss</id>
    <name>Hanns Holger Rutz</name>
    <url>http://www.sciss.de</url>
  </developer>
</developers>
}
