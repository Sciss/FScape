![logo](http://sciss.de/fscape/application.png)

# FScape

[![Gitter](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/Sciss/FScape?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
[![Build Status](https://travis-ci.org/Sciss/FScape.svg?branch=master)](https://travis-ci.org/Sciss/FScape)

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

## downloading

A binary version of FScape for all platforms is provided through 
[archive.org](https://archive.org/details/FScape) or [GitHub Releases](https://github.com/Sciss/FScape/releases/latest).

__Note:__ A new version of FScape (["next"](https://git.iem.at/sciss/FScape-next)) is being developed based on a UGen graph in Scala. Over time, it will
accumulate modules translated from FScape 1 as well as new modules. You can find these modules in the file
`FScape-modules.zip`, a workspace for [Mellite](https://sciss.de/mellite), distributed with the Mellite downloads.

<img src="screenshot.png" alt="screenshot" width="648" height="363"/>

## running

In the binary distribution, you should use the shell scripts `bin/fscape` (Linux, OS X) or `bin/fscape.bat` (Windows)
to start the application. If you have installed the Debian package, `fscape` should be on your path and available as a
desktop icon in your desktop environment.

__Note Mac users:__ You need a Java 6 installation or higher (Java 8 is recommended as of this writing). You can
install Java 8 from [Oracle](https://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html).
Install the full JDK not just the JRE runtime. Also, it has been reported that there are problems when the installation
location of FScape (the directory you put it in, or its parent directories) contains space characters.

## compiling

FScape builds with [sbt](http://www.scala-sbt.org/). You can use the provided `sbt` shell script if you do not want
to install sbt on your system.

 - to compile: `sbt compile`
 - to package: `sbt package`
 - to run: `sbt run`
 - to make a standalone jar: `sbt assembly`
 
The release bundles are produced with tasks `universal:packageBin` and `debian:packageBin`.

## contributing

Please see the file [CONTRIBUTING.md](CONTRIBUTING.md) for details.

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

## documentation

A basic quick start guide starts from `help/index.html`. This help is also accessible from the help menu within the
application. For each of the processing modules, help is available via Help &gt; Module Documentation.

A short screencast is available on [Vimeo](https://vimeo.com/26509124).

For help, visit the Gitter channel.
