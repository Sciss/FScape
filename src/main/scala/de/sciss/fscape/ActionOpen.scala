/*
 *  ActionOpen.scala
 *  (FScape)
 *
 *  Copyright (c) 2001-2015 Hanns Holger Rutz. All rights reserved.
 *
 *  This software is published under the GNU General Public License v3+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 */

package de.sciss.fscape

import swing.{Dialog, Action}
import de.sciss.desktop.{Menu, RecentFiles, FileDialog, KeyStrokes}
import scala.util.control.NonFatal
import de.sciss.file._
import language.existentials
import scala.swing.event.Key
import de.sciss.fscape.{FScape => App}
import de.sciss.fscape.prop.BasicProperties
import de.sciss.fscape.session.ModulePanel

object ActionOpen extends Action("Open...") {
  import KeyStrokes._

  private val _recent = RecentFiles(App.userPrefs("recent-docs")) { folder =>
    perform(folder)
  }

  accelerator = Some(menu1 + Key.O)

  private def fullTitle = "Open Document"

  def recentFiles: RecentFiles  = _recent
  def recentMenu : Menu.Group   = _recent.menu

  def apply(): Unit = {
    val dlg = FileDialog.open(title = fullTitle)
    // dlg.setFilter { f => f.isDirectory && f.ext.toLowerCase == Workspace.ext }
    dlg.show(None).foreach(perform)
  }

  def perform(f: File): Unit =
    App.documentHandler.documents.find(_.getFile == f).fold(openRead(f)) { doc =>
      App.documentViewHandler.getWindow(doc)
        .foreach(_.front())
    }

  private def openRead(f: File): Unit =
    try {
      val preset = new BasicProperties(null, f)
      preset.load()
      val className = preset.getProperty(ModulePanel.PROP_CLASS)
      if (className != null) {
        //        if (className.startsWith("fscape.")) {
        //          // pre version 0.67
        //          className = "de.sciss." + className;
        //        }
        val keyI  = className.lastIndexOf('.')
        val key   = className.substring(keyI + 1, className.length - 3)
        App.newDocument(key).foreach { mod =>
          mod.loadFile(f)
          recentFiles.add(mod.getDocument.getFile)  // must be after loadFile
        }

      } else {
        Dialog.showMessage(
          message     = s"Wrong file format. Missing entry '${ModulePanel.PROP_CLASS}'",
          title       = fullTitle,
          messageType = Dialog.Message.Error
        )
      }

    } catch {
      case NonFatal(e) =>
        Dialog.showMessage(
          message     = s"Unable to create new document ${f.path}\n\n${GUI.formatException(e)}",
          title       = fullTitle,
          messageType = Dialog.Message.Error
        )
    }
}