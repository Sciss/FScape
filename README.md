![logo](http://sciss.de/fscape/application.png)

# FScape

[![Gitter](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/Sciss/FScape?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
[![Build Status](https://travis-ci.org/Sciss/FScape.svg?branch=master)](https://travis-ci.org/Sciss/FScape)
<a href="https://liberapay.com/sciss/donate"><img alt="Donate using Liberapay" src="https://liberapay.com/assets/widgets/donate.svg" height="24"></a>

## statement

FScape is a standalone, cross-platform audio rendering software.

FScape is (C)opyright 2001&ndash;2019 by Hanns Holger Rutz. All rights reserved.

This program is free software; you can redistribute it and/or modify it under the terms of 
the [GNU General Public License](https://git.iem.at/sciss/FScape/blob/master/LICENSE) v3+.

This program is distributed in the hope that it will be useful, but _without any warranty_ without even the implied
warranty of _merchantability_ or _fitness for a particular purpose_. See the GNU General Public License for more details.

To contact the author, send an email to `contact at sciss.de`. For project status, API and current version
visit [git.iem.at/sciss/FScape](http://git.iem.at/sciss/FScape).

FScape is winner of the 2014 [LoMus award](http://concours.afim-asso.org/) (ex-aequo) by the Association
Française d’Informatique Musicale (AFIM).

Please consider supporting this project through Liberapay (see badge above) – thank you!

## download and installation

- A binary (executable) version is provided via [archive.org](https://archive.org/details/FScape) or
  [GitHub Releases](https://github.com/Sciss/FScape/releases/latest).
  We provide a universal zip for all platforms as well as JDK bundled versions for Linux, Windows, Mac,
  and a dedicated package for Debian / Ubuntu.
- The source code can be downloaded from [git.iem.at/sciss/FScape](https://git.iem.at/sciss/FScape) or 
  [github.com/Sciss/FScape](http://github.com/Sciss/FScape).

In order to run the application, you must have a Java Development Kit (JDK) installed or use one of the bundled ("full") downloads. The recommended versions
are __JDK 8.__ and __JDK 11__. https://adoptopenjdk.net/ is a good source for JVM installers.

__Note:__ A new version of FScape (["next"](https://git.iem.at/sciss/FScape-next)) is being developed based on a UGen graph in Scala. Over time, it will
accumulate modules translated from FScape 1 as well as new modules. You can find these modules in the file
`FScape-modules.zip`, a workspace for [Mellite](https://sciss.de/mellite), distributed with the Mellite downloads.

<img src="screenshot.png" alt="screenshot" width="648" height="363"/>

## running

In the binary distribution, you should use the shell scripts `bin/fscape` (Linux, OS X) or `bin/fscape.bat` (Windows)
to start the application. If you have installed the Debian package, `fscape` should be on your path and available as a
desktop icon in your desktop environment.

__Note Mac users:__ If you download the plain version without bundled JDK, there is a high chance that your system has a too old Java installation, typically indicated by a "java.lang.UnsupportedClassVersionError". See above for links to download JDK 8 or 11.

## documentation

A basic quick start guide starts from `help/index.html`. This help is also accessible from the help menu within the
application. For each of the processing modules, help is available via Help &gt; Module Documentation.

An old but still useful short screencast is available on [Vimeo](https://vimeo.com/26509124).

For help, visit the [Gitter channel](https://gitter.im/Sciss/FScape).

## contributing

Please see the file [CONTRIBUTING.md](CONTRIBUTING.md) for details.

--------

## building from source

See the section 'download and installation' for requirements (JDK).

FScape builds with [sbt](http://www.scala-sbt.org/). You can use the provided `sbt` shell script if you do not want
to install sbt on your system.

 - to compile: `sbt compile`
 - to package: `sbt package`
 - to run: `sbt run`
 - to make a standalone jar: `sbt assembly`

The standalone jar, created via `./sbt fscape/assembly` produces `FScape.jar` which is double-clickable and can be run via:

    $ java -jar FScape.jar

Runnable packages can be created via `./sbt universal:packageBin` (all platforms) or `./sbt debian:packageBin` (Debian).

## building with bundled JDK

We are currently experimenting with a build variant that bundles the JDK using the JLink plugin for sbt-native-packager.
In order to build this version, run `sbt fscape-full/universal:packageBin`.
The produced installation is _platform dependent_, so will create a version that only works on the OS you are building from.

Note that should probably specify an explicit java-home, otherwise the bundled package might be unreasonably large:

    sbt -java-home ~/Downloads/OpenJDK11U-jdk_x64_linux_hotspot_11.0.4_11/jdk-11.0.4+11 fscape-full/clean fscape-full/update fscape-full/debian:packageBin

## source code distribution

FScape's GPL'ed source code is made available through [git.iem.at/sciss/FScape](http://git.iem.at/sciss/FScape).

For OSC communication, FScape uses the NetUtil library, which is licensed under the GNU Lesser General Public
License (LGPL). The source code is available from [git.iem.at/sciss/NetUtil](https://git.iem.at/sciss/NetUtil).

FScape uses the ScissLib library which is licensed under the GNU General Public License, source code provided
through [git.iem.at/sciss/ScissLib](https://git.iem.at/sciss/ScissLib).


FScape is bundled with the Web Look-and-feel licensed under the GNU General Public License, source code provided
through [github.com/mgarin/weblaf](https://github.com/mgarin/weblaf).

The libraries [Desktop](https://git.iem.at/sciss/Desktop), [FileUtil](https://git.iem.at/sciss/FileUtil)
and [RaphaelIcons](https://git.iem.at/sciss/RaphaelIcons) are covered by the LGPL.

The [sbt build script](https://github.com/paulp/sbt-extras) by Paul Phillips is included which is licensed under
a BSD style license.

---------

## creating new releases

This section is an aide-mémoire for me in releasing stable versions.

We're currently publishing the following artifacts:

 - `fsacpe_<version>_all.zip`
 - `fscape-full_<version>_linux_x64.zip`
 - `fscape-full_<version>_amd64.deb`
 - `fscape-full_<version>_win_x64.zip`
 - `fscape-full_<version>_mac_x64.zip`

To build for Linux:

 1. `sbt fscape/universal:packageBin`
 2. `sbt -java-home '/home/hhrutz/Downloads/OpenJDK11U-jdk_x64_linux_hotspot_11.0.4_11/jdk-11.0.4+11' fscape-full/universal:packageBin fscape-full/debian:packageBin`
 
Copy the artifacts to a safe location now.
To build for Mac and Windows, we need to publish all libraries now to Maven Central.
Then Windows can be built on Linux using wine:
 
 1. `rm -r full/target` (otherwise Jlink fails)
 2. `wine cmd.exe` and
 `Z:\home\hhrutz\Downloads\OpenJDK11U-jdk_x64_windows_hotspot_11.0.4_11\jdk-11.0.4+11\bin\java.exe -jar Z:\home\hhrutz\Downloads\sbt-1.2.8\sbt\bin\sbt-launch.jar` then in sbt console:
 `project fscape-full` and `universal:packageBin`
 
For Mac we need a bloody fruit company machine:

 1. `git fetch; git merge origin/work`
 2. `./sbt -java-home /Users/naya/Downloads/jdk-11.0.4+11/Contents/Home fscape-full/clean fscape-full/update fscape-full/universal:packageBin`
 3. We need to set the execution bits on Linux after copying the zip to the Linux machine, and unpacking it:
 `rm fscape-full_<version>_mac_x64/bin/fscape.bat` then
 `rm fscape-full_<version>_mac_x64.zip` then
 `chmod a+x fscape-full_<version>_mac_x64/bin/fscape` then
 `chmod a+x fscape-full_<version>_mac_x64/jre/bin/*`
 4. Repackage: `zip -y -r -9 fscape-full_<version>_mac_x64.zip fscape-full_<version>_mac_x64`

