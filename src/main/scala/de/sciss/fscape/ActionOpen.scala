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

  /** Registers the document with the recent files menu and the document handler.
    * Does _not_ open a view directly. This should be done by listening to the document handler.
    */
  def openGUI(doc: App.Document): Unit = {
    recentFiles.add(doc.getFile)
    App.documentHandler.addDocument(doc)
  }

  def recentFiles: RecentFiles  = _recent
  def recentMenu : Menu.Group   = _recent.menu

  def apply(): Unit = {
    val dlg = FileDialog.open(title = fullTitle)
    // dlg.setFilter { f => f.isDirectory && f.ext.toLowerCase == Workspace.ext }
    dlg.show(None).foreach(perform)
  }

  def perform(f: File): Unit =
    App.documentHandler.documents.find(_.getFile == f).fold(openRead(f)) { doc =>
      // DocumentViewHandler.instance.getWindow(doc)
      //   .foreach(_.window.front())
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
          openGUI(mod.getDocument)
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
          message     = s"Unable to create new document ${f.path}\n\n${formatException(e)}",
          title       = fullTitle,
          messageType = Dialog.Message.Error
        )
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

  private def formatException(e: Throwable): String = {
    e.getClass.toString + " :\n" + wordWrap(e.getMessage) + "\n" +
      e.getStackTrace.take(10).map("   at " + _).mkString("\n")
  }
}