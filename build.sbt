import AssemblyKeys._ // put this at the top of the file

name             := "FScape"

version          := "1.0.1-SNAPSHOT"

organization     := "de.sciss"

description      := "A standalone audio rendering software for time domain and spectral signal processing"

homepage         := Some(url("https://github.com/Sciss/" + name.value))

licenses         := Seq("GPL v2+" -> url("http://www.gnu.org/licenses/gpl-2.0.txt"))

scalaVersion     := "2.10.3"

crossPaths       := false  // this is just a Java project right now!

autoScalaLibrary := false

// retrieveManaged := true

mainClass in Compile := Some("de.sciss.fscape.Main")

// ---- bundling ----

seq(assemblySettings: _*)

test    in assembly    := ()

target  in assembly    := baseDirectory.value

jarName in assembly    := s"${name.value}.jar"

seq(appbundle.settings: _*)

appbundle.javaOptions ++= Seq("-Xmx1024m", "-ea")

appbundle.icon         := Some(file("icons") / "application.icns")

appbundle.target       := baseDirectory.value

appbundle.mainClass    := (mainClass in Compile).value

appbundle.signature    := "FSc "

appbundle.documents    += appbundle.Document(
  name       = "FScape Document",
  role       = appbundle.Document.Editor,
  icon       = Some(file("icons") / "document.icns"),
  extensions = Seq("fsc")
)

// ---- publishing ----

publishMavenStyle := true

publishTo :=
  Some(if (version.value endsWith "-SNAPSHOT")
    "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
  else
    "Sonatype Releases"  at "https://oss.sonatype.org/service/local/staging/deploy/maven2"
  )

publishArtifact in Test := false

pomIncludeRepository := { _ => false }

pomExtra := { val n = name.value
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
