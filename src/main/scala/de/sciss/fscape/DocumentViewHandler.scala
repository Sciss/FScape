/*
 *  DocumentViewHandler.scala
 *  (FScape)
 *
 *  Copyright (c) 2001-2021 Hanns Holger Rutz. All rights reserved.
 *
 *  This software is published under the GNU General Public License v3+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 */

package de.sciss.fscape

import de.sciss.fscape.session.{Session => Document}
import de.sciss.fscape.{FScape => App}

class DocumentViewHandler {
  private val DEBUG = false

  private var map = Map.empty[Document, DocumentWindow]

  def getWindow(doc: Document): Option[DocumentWindow] = map.get(doc)

  def windows: Iterator[DocumentWindow] = map.values.iterator

  private def log(what: => String): Unit = if (DEBUG) println(s"<doc-view-handler> $what")

  private[fscape] def addWindow(w: DocumentWindow): Unit = {
    log(s"Add ${w.document.getName}")
    map += w.document -> w
  }

  private[fscape] def removeWindow(w: DocumentWindow): Unit = {
    log(s"Remove ${w.document.getName}")
    map -= w.document
  }
}
