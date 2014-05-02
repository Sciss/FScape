package de.sciss.fscape

import de.sciss.fscape.{FScape => App}

class DocumentViewHandler {
  private val DEBUG = false

  private var map = Map.empty[App.Document, DocumentWindow]

  def getWindow(doc: App.Document): Option[DocumentWindow] = map.get(doc)

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
