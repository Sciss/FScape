import com.typesafe.sbt.packager.linux.LinuxPackageMapping

lazy val baseName     = "FScape"
lazy val baseNameL    = baseName.toLowerCase

def appName  = baseName
def appNameL = baseNameL

lazy val appVersion = "1.8.1"

lazy val authorName   = "Hanns Holger Rutz"
lazy val authorEMail  = "contact@sciss.de"

lazy val appDescription = "A standalone audio rendering software for time domain and spectral signal processing"

lazy val appMainClass = Some("de.sciss.fscape.FScape")

lazy val basicJavaOpts = Seq("-source", "1.8")

lazy val deps = new {
  val main = new {
    val desktop  = "0.11.3"
    val fileUtil = "1.1.5"
    val netUtil  = "1.1.0"
    val pegDown  = "1.6.0"
    val raphael  = "1.0.7"
    val scissLib = "1.1.5"
    val submin   = "0.3.4"
  }
}

lazy val commonSettings = Seq(
  name             := baseName,
  version          := appVersion,
  organization     := "de.sciss",
  description      := appDescription,
  homepage         := Some(url(s"https://github.com/Sciss/${name.value}")),
  licenses         := Seq("GPL v3+" -> url("http://www.gnu.org/licenses/gpl-3.0.txt")),
  scalaVersion     := "2.13.6",
  javacOptions    ++= basicJavaOpts ++ Seq("-encoding", "utf8", "-Xlint:unchecked", "-target", "1.8"),
  javacOptions in (Compile, doc) := basicJavaOpts,  // does not accept `-encoding` or `target`
  mainClass in Compile := appMainClass,
  libraryDependencies ++= Seq(
    "de.sciss"    %  "submin"             % deps.main.submin,
    "de.sciss"    %% "desktop-core"       % deps.main.desktop,
    "de.sciss"    %% "desktop-linux"      % deps.main.desktop,
    "de.sciss"    %% "desktop-mac"        % deps.main.desktop,
    "de.sciss"    %% "raphael-icons"      % deps.main.raphael,
    "de.sciss"    %% "fileutil"           % deps.main.fileUtil,
    "de.sciss"    %  "scisslib"           % deps.main.scissLib,
    "de.sciss"    %  "netutil"            % deps.main.netUtil,
    "org.pegdown" %  "pegdown"            % deps.main.pegDown,
  )
)

// ---- bundling ----

lazy val assemblySettings = Seq(
  mainClass       in assembly    := appMainClass,
  test            in assembly    := {},
  target          in assembly    := baseDirectory.value,
  assemblyJarName in assembly    := s"$baseName.jar"
)

// ---- build info source generator ----

lazy val buildInfoSettings = Seq(
  buildInfoKeys := Seq(name, organization, version, scalaVersion, description,
    BuildInfoKey.map(homepage) { case (k, opt) => k -> opt.get },
    BuildInfoKey.map(licenses) { case (_, Seq((lic, _))) => "license" -> lic }
  ),
  buildInfoPackage := "de.sciss.fscape"
)

// ---- publishing ----

lazy val publishSettings = Seq(
  publishMavenStyle := true,
  publishTo := {
    Some(if (isSnapshot.value)
      "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
    else
      "Sonatype Releases"  at "https://oss.sonatype.org/service/local/staging/deploy/maven2"
    )
  },
  publishArtifact in Test := false,
  pomIncludeRepository := { _ => false },
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
)

//////////////// universal (directory) installer
lazy val pkgUniversalSettings = Seq(
  executableScriptName := appNameL,
//  // NOTE: doesn't work on Windows, where we have to
//  // provide manual file `SYSSON_config.txt` instead!
//  javaOptions in Universal ++= Seq(
//    // -J params will be added as jvm parameters
//    "-J-Xmx1024m"
//    // others will be added as app parameters
//    // "-Dproperty=true",
//  ),
  // Since our class path is very very long,
  // we use instead the wild-card, supported
  // by Java 6+. In the packaged script this
  // results in something like `java -cp "../lib/*" ...`.
  // NOTE: `in Universal` does not work. It therefore
  // also affects debian package building :-/
  // We need this settings for Windows.
  scriptClasspath /* in Universal */ := Seq("*"),
  name        in Linux := appName,
  packageName in Linux := appNameL,
  mainClass   in Universal := appMainClass,
  maintainer  in Universal := s"$authorName <$authorEMail>",
  target      in Universal := (target in Compile).value,
)

//////////////// debian installer
lazy val pkgDebianSettings = Seq(
  packageName        in Debian := appNameL,
  packageSummary     in Debian := appDescription,
  mainClass          in Debian := appMainClass,
  maintainer         in Debian := s"$authorName <$authorEMail>",
  packageDescription in Debian :=
    """The audio rendering suite FScape consists of around fifty
      | independent modules for rendering audio files. From simple
      | utilities such as separating channels, normalising, cutting
      | and splicing sounds, through various DSP and filtering algorithms
      | to more complex algorithmic units which take a sound, analyse it,
      | and rearrange it in new forms. Many of the processes and their ways
      | of parametrisation are unique.
      |""".stripMargin,
  // include all files in src/debian in the installed base directory
  linuxPackageMappings in Debian ++= {
    val n     = appNameL // (name            in Debian).value.toLowerCase
    val dir   = (sourceDirectory in Debian).value / "debian"
    val f1    = (dir * "*").filter(_.isFile).get  // direct child files inside `debian` folder
    val f2    = ((dir / "doc") * "*").get
    //
    def readOnly(in: LinuxPackageMapping) =
      in.withUser ("root")
        .withGroup("root")
        .withPerms("0644")  // http://help.unc.edu/help/how-to-use-unix-and-linux-file-permissions/
    //
    val aux   = f1.map { fIn => packageMapping(fIn -> s"/usr/share/$n/${fIn.name}") }
    val doc   = f2.map { fIn => packageMapping(fIn -> s"/usr/share/doc/$n/${fIn.name}") }
    (aux ++ doc).map(readOnly)
  }
)

lazy val root = project.withId(baseNameL).in(file("."))
  .enablePlugins(BuildInfoPlugin)
  .enablePlugins(JavaAppPackaging, DebianPlugin)
  .settings(commonSettings)
  .settings(buildInfoSettings)
  .settings(assemblySettings)
  .settings(pkgUniversalSettings)
  .settings(useNativeZip) // cf. https://github.com/sbt/sbt-native-packager/issues/334
  .settings(pkgDebianSettings)
  .settings(publishSettings)
  .settings(
    packageName               in Universal := s"${appNameL}_${version.value}_all",
    name                      in Debian    := appNameL,
    debianPackageDependencies in Debian   ++= Seq("java8-runtime"),
  )

// Determine OS version of JavaFX binaries
lazy val jfxClassifier = sys.props("os.name") match {
  case n if n.startsWith("Linux")   => "linux"
  case n if n.startsWith("Mac")     => "mac"
  case n if n.startsWith("Windows") => "win"
  case _ => throw new Exception("Unknown platform!")
}

def archSuffix: String =
  sys.props("os.arch") match {
    case "i386"  | "x86_32" => "x32"
    case "amd64" | "x86_64" => "x64"
    case other              => other
  }

lazy val full = project.withId(s"$baseNameL-full").in(file("full"))
  .dependsOn(root)
  .enablePlugins(JavaAppPackaging, DebianPlugin, JlinkPlugin)
  .settings(commonSettings)
  .settings(pkgUniversalSettings)
  .settings(pkgDebianSettings)
  // disabled so we don't need to install zip.exe on wine:
  // .settings(useNativeZip) // cf. https://github.com/sbt/sbt-native-packager/issues/334
  .settings(assemblySettings) // do we need this here?
//  .settings(appSettings)
  .settings(
    name := s"$baseName-full",
    version := appVersion,
    jlinkIgnoreMissingDependency := JlinkIgnore.everything, // temporary for testing
    jlinkModules += "jdk.unsupported", // needed for JFileChooser / WebLaF
//    libraryDependencies ++= Seq("base", "swing", "controls", "graphics", "media", "web").map(jfxDep),
    packageName in Universal := s"${appNameL}-full_${version.value}_${jfxClassifier}_$archSuffix",
    name                in Debian := s"$appNameL-full",  // this is used for .deb file-name; NOT appName,
    packageArchitecture in Debian := sys.props("os.arch"), // archSuffix,
  )

