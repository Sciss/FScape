package de.sciss.desktop

import java.util.prefs

object Escape {
  def prefsPeer(p: Preferences): prefs.Preferences = p.peer
}
