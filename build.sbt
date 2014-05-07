import AssemblyKeys._ // put this at the top of the file

name             := "FScape"

version          := "1.1.1"

organization     := "de.sciss"

description      := "A standalone audio rendering software for time domain and spectral signal processing"

homepage         := Some(url("https://github.com/Sciss/" + name.value))

licenses         := Seq("GPL v3+" -> url("http://www.gnu.org/licenses/gpl-3.0.txt"))

scalaVersion     := "2.11.0"

javacOptions    ++= Seq("-source", "1.6", "-target", "1.6")

// retrieveManaged := true

mainClass in Compile := Some("de.sciss.fscape.FScape")

libraryDependencies ++= Seq(
  "de.sciss"    %  "weblaf"             % "1.27",
  "de.sciss"    %% "desktop-mac"        % "0.5.2",
  // "de.sciss" %% "audiowidgets-swing" % "1.6.2",
  "de.sciss"    %% "raphael-icons"      % "1.0.2",
  "de.sciss"    %% "fileutil"           % "1.1.1",
  "de.sciss"    %  "scisslib"           % "1.0.0",
  "de.sciss"    %  "netutil"            % "1.0.0",
  "org.pegdown" %  "pegdown"            % "1.4.2"
)

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

// ---- build info source generator ----

buildInfoSettings

sourceGenerators in Compile <+= buildInfo

buildInfoKeys := Seq(name, organization, version, scalaVersion, description,
  BuildInfoKey.map(homepage) { case (k, opt) => k -> opt.get },
  BuildInfoKey.map(licenses) { case (_, Seq((lic, _))) => "license" -> lic }
)

buildInfoPackage := "de.sciss.fscape"

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
