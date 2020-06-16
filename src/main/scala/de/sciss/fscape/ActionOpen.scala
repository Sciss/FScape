/*
 *  ActionOpen.scala
 *  (FScape)
 *
 *  Copyright (c) 2001-2020 Hanns Holger Rutz. All rights reserved.
 *
 *  This software is published under the GNU General Public License v3+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 */

package de.sciss.fscape

import de.sciss.desktop.{FileDialog, KeyStrokes, Menu, RecentFiles}
import de.sciss.file._
import de.sciss.fscape.prop.BasicProperties
import de.sciss.fscape.session.{ModulePanel, Session => Document}
import de.sciss.fscape.{FScape => App}

import scala.language.existentials
import scala.swing.event.Key
import scala.swing.{Action, Dialog}
import scala.util.{Failure, Success, Try}

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

  def perform(f: File): Unit = perform(f, visible = true)

  def perform(f: File, visible: Boolean): Try[Document] =
    App.documentHandler.documents.find(_.getFile == f).fold[Try[Document]](openRead(f, visible = visible)) { doc =>
      if (visible) App.documentViewHandler.getWindow(doc).foreach(_.front())
      Success(doc)
    }

  private def openRead(f: File, visible: Boolean): Try[Document] = {
    def showError(message: String): Unit =
      Dialog.showMessage(
        message     = message,
        title       = fullTitle,
        messageType = Dialog.Message.Error
      )

    case class MissingClass()
      extends RuntimeException(s"Wrong file format. Missing entry '${ModulePanel.PROP_CLASS}'")

    val res: Try[Document] = Try {
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
        val doc   = App.newDocument(key, visible = visible)
        doc.foreach { mod =>
          mod.loadFile(f)
          recentFiles.add(mod.getDocument.getFile)  // must be after loadFile
        }
        doc.get.getDocument

      } else {
        throw MissingClass()
      }
    }

    if (visible) res match {
      case Failure(e @ MissingClass()) =>
        showError(e.getMessage)
      case Failure(e) =>
        showError(s"Unable to create new document ${f.path}\n\n${GUI.formatException(e)}")
      case _ =>
    }

    res
  }
}