/*
 *  GUI.scala
 *  (FScape)
 *
 *  Copyright (c) 2001-2016 Hanns Holger Rutz. All rights reserved.
 *
 *  This software is published under the GNU General Public License v3+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 */

package de.sciss.fscape

import java.awt.GraphicsEnvironment
import de.sciss.desktop.Window
import scala.swing.Swing
import Swing._

object GUI {
  def centerOnScreen(w: Window): Unit = placeWindow(w, 0.5f, 0.5f, 0)

  def placeWindow(w: Window, horizontal: Float, vertical: Float, padding: Int): Unit = {
    val ge  = GraphicsEnvironment.getLocalGraphicsEnvironment
    val bs  = ge.getMaximumWindowBounds
    val b   = w.size
    val x   = (horizontal * (bs.width  - padding * 2 - b.width )).toInt + bs.x + padding
    val y   = (vertical   * (bs.height - padding * 2 - b.height)).toInt + bs.y + padding
    w.location = (x, y)
  }


  private def wordWrap(s: String, margin: Int = 80): String = {
    if (s == null) return "" // fuck java
    val sz = s.length
    if (sz <= margin) return s
    var i = 0
    val sb = new StringBuilder
    while (i < sz) {
      val j = s.lastIndexOf(" ", i + margin)
      val found = j > i
      val k = if (found) j else i + margin
      sb.append(s.substring(i, math.min(sz, k)))
      i = if (found) k + 1 else k
      if (i < sz) sb.append('\n')
    }
    sb.toString()
  }

  def formatException(e: Throwable): String = {
    val stack = e.getStackTrace.take(10).map("   at " + _).mkString("\n")
    s"${e.getClass} :\n${wordWrap(e.getMessage)}\n$stack"
  }
}
