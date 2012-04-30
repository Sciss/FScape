import AssemblyKeys._ // put this at the top of the file

name := "FScape"

version := "0.75-SNAPSHOT"

organization := "de.sciss"

description := "A standalone audio rendering software for time domain and spectral signal processing"

homepage := Some( url( "https://github.com/Sciss/FScape" ))

licenses := Seq( "GPL v2+" -> url( "http://www.gnu.org/licenses/gpl-2.0.txt" ))

scalaVersion := "2.9.2"

crossPaths := false  // this is just a Java project right now!

retrieveManaged := true

// ---- bundling ----

seq( assemblySettings: _* )

test in assembly := {}

seq( appbundle.settings: _* )

appbundle.javaOptions ++= Seq( "-Xmx1024m", "-ea" )

appbundle.icon := Some( file( "FScape.icns" ))

// ---- publishing ----

publishMavenStyle := true

publishTo <<= version { (v: String) =>
   Some( if( v.endsWith( "-SNAPSHOT" ))
      "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
   else
      "Sonatype Releases"  at "https://oss.sonatype.org/service/local/staging/deploy/maven2"
   )
}

publishArtifact in Test := false

pomIncludeRepository := { _ => false }

pomExtra :=
<scm>
  <url>git@github.com:Sciss/FScape.git</url>
  <connection>scm:git:git@github.com:Sciss/FScape.git</connection>
</scm>
<developers>
   <developer>
      <id>sciss</id>
      <name>Hanns Holger Rutz</name>
      <url>http://www.sciss.de</url>
   </developer>
</developers>
