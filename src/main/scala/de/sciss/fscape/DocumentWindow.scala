package de.sciss.fscape

import de.sciss.fscape.session.ModulePanel
import de.sciss.desktop.impl.WindowImpl
import de.sciss.desktop._
import scala.swing._
import de.sciss.file._
import de.sciss.io.IOUtil
import de.sciss.fscape.{FScape => App}
import javax.swing.SwingUtilities
import de.sciss.swingplus.OverlayPanel
import de.sciss.desktop.Window
import de.sciss.desktop.Menu
import Swing._
import de.sciss.fscape.proc.{ProcessorEvent, ProcessorAdapter}
import scala.io.Source
import scala.util.control.NonFatal

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
    "file.close"    -> Action(null)(tryClose()),
    "help.module"   -> Action(null)(moduleHelp())
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

  private def moduleHelp(): Unit = {
    val prefix0 = module.getClass.getName
    val prefix  = prefix0.substring(prefix0.lastIndexOf('.') + 1, prefix0.length - 3)
    val title   = "Module Help"
    try {
      // println(prefix)
      val url = App.getClass.getResource(s"help/$prefix.md")
      if (url == null) {
        Dialog.showMessage(
          message     = s"No module help file found for ${module.getModuleName}.",
          title       = title,
          messageType = Dialog.Message.Warning
        )
        return
      }
      val md = Source.fromFile(url.toURI, "UTF-8").mkString
      App.browseMarkdown(title0 = title, source = md)

    } catch {
      case NonFatal(e) =>
        Dialog.showMessage(
          message     = s"Unable to create module help file for ${module.getModuleName}\n\n${GUI.formatException(e)}",
          title       = title,
          messageType = Dialog.Message.Error
        )
    }
  }

  override def dispose(): Unit = {
    super.dispose()
    // XXX TODO: gWindow.foreach(_.remove(idShow))
    App.documentViewHandler.removeWindow(this)
    App.documentHandler.removeDocument(document)
    document.dispose()
  }

  def tryClose(title: String = "Close Document"): Boolean = {
    if (module.isRunning) {
      def closeDialog(): Unit = {
        val w = SwingUtilities.getWindowAncestor(opt.peer)
        w.dispose()
      }

      lazy val ggCancel: Button = Button("Cancel")(closeDialog())

      lazy val ggBusy: ProgressBar = new ProgressBar {
        preferredSize = {
          val d = ggCancel.preferredSize
          d.width = 32
          d
        }
        indeterminate = true
        visible       = false
      }

      lazy val pOver: OverlayPanel = new OverlayPanel {
        contents += HStrut(32)
        contents += ggBusy
      }

      lazy val ggStop: Button = Button("Abort Rendering") {
        ggBusy.visible  = true
        ggStop.text     = "Aborting..."
        module.stop()
      }

      lazy val entries = Seq(HStrut(32), ggCancel, ggStop, pOver)
      lazy val opt: OptionPane[OptionPane.Result.Value] =
        OptionPane(message = s"<html><b>${window.title}</b><p>The module is rendering.",
          optionType = OptionPane.Options.OkCancel,
          messageType = OptionPane.Message.Question, entries = entries)
      opt.title = "Close Document"
      front()

      val li = new ProcessorAdapter {
        override def processorStopped(e: ProcessorEvent): Unit = {
          closeDialog()
        }
      }

      module.addProcessorListener(li)
      /* val res = */ opt.show(Some(this))
      module.removeProcessorListener(li)
      // println(s"Result: $res")
    }

    val ok = !module.isRunning
    if (ok) dispose()
    ok
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
