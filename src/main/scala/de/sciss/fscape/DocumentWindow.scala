package de.sciss.fscape

import de.sciss.fscape.session.ModulePanel
import de.sciss.desktop.impl.WindowImpl
import de.sciss.desktop._
import scala.swing.{Button, Action, Component}
import de.sciss.file._
import de.sciss.io.IOUtil
import de.sciss.fscape.{FScape => App}
import scala.Some

final class DocumentWindow(val module: ModulePanel) extends WindowImpl {
  window =>

  def handler: WindowHandler = App.windowHandler

  contents  = Component.wrap(module)

  def updateTitle(): Unit = {
    val m             = module.getModuleName
    val fOpt          = Option(document.getFile)
    val tit           = fOpt.fold(m) { f => s"$m - ${f.base}" }
    title             = tit
    ActionShow.title  = tit
  }

  updateTitle() // important: must be before binding the menu items

  bindMenus(
    "file.save"     -> ActionSave,
    "file.save-as"  -> ActionSaveAs,
    "file.close"    -> Action(null)(tryClose())
  )

  private def gWindow: Option[Menu.Group] = App.menuRoot.get("window") match {
    case Some(mg: Menu.Group) => Some(mg)
    case _ => None
  }

  private def idShow: String = s"doc-${document.getNodeID}"

  gWindow.foreach(_.add(Menu.Item(idShow, ActionShow)))

  pack()
  // GUI.centerOnScreen(this)
  GUI.placeWindow(this, 0.5f, 0.333f, 0)
  App.documentViewHandler.addWindow(this)

  closeOperation = Window.CloseIgnore

  reactions += {
    case Window.Closing(_) => tryClose()
  }

  override def dispose(): Unit = {
    super.dispose()
    // XXX TODO: gWindow.foreach(_.remove(idShow))
    App.documentViewHandler.removeWindow(this)
    App.documentHandler.removeDocument(document)
    document.dispose()
  }

  def tryClose(): Boolean = {
    if (module.isRunning) {
      val ggStop  = Button("Abort Rendering") {
        println("abort...")
      }
      val ggCancel = Button("Cancel")()
      val entries = Seq(ggStop, ggCancel)
      val opt = OptionPane(message = s"<html><b>${document.getName}</b><p>The module is rendering.",
        optionType = OptionPane.Options.OkCancel,
        messageType = OptionPane.Message.Question, entries = entries)
      opt.title = "Close Document"
      front()
      val res = opt.show(Some(this))
      println(s"Result: $res")
    }
    dispose()
    true
  }

  def document: App.Document = module.getDocument

  private def saveFile(f: File): Unit =
    if (module.saveFile(f)) {
      updateTitle()
      file  = Some(f)
      // dirty = false
      ActionOpen.recentFiles.add(f)
    }

  private object ActionSave extends Action("Save") {
    def apply(): Unit = {
      val doc = document
      val f0  = Option(doc.getFile)
      val f   = f0.orElse(ActionSaveAs.query(None))
      f.foreach(saveFile)
    }
  }

  private object ActionSaveAs extends Action("Save As...") {
    def apply(): Unit = {
      query(Option(document.getFile)).foreach(saveFile)
    }

    /** Opens a file chooser so the user
      * can select a new output file and format for the session.
      *
      * @return the AudioFileDescr representing the chosen file path
      *         and format or <code>None</code>
      *         if the dialog was cancelled.
      */
    def query(protoType: Option[File]): Option[File] = {
      val f0 = protoType.getOrElse {
        val dir = userHome / "Documents"
        val f1 = if (dir.isDirectory) dir / "Untitled.fsc" else new File("Untitled.fsc")
        IOUtil.nonExistentFileVariant(f1, -1, " ", null)
      }

      val f = f0.replaceExt("fsc")

      val fDlg = FileDialog.save(init = Some(f), title = "Save Document")
      fDlg.show(Some(window))
    }
  }

  private object ActionShow extends Action(null) {
    def apply(): Unit = front()
  }
}
