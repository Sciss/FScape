package de.sciss.fscape
package impl

import de.sciss.file.file
import de.sciss.fscape.FScape.Document
import de.sciss.fscape.net.{OSCRoot, OSCRouter, OSCRouterWrapper, RoutedOSCMessage}
import de.sciss.fscape.session.Session

import scala.util.{Success, Failure}

object DocumentHandlerImpl {
  private final val OSC_DOC			= "doc"
  // sub level
  private final val OSC_ACTIVE	= "active"
  private final val OSC_INDEX		= "index"
  private final val OSC_ID			= "id"

  //	public static interface OpenDoneHandler {
  //		public void openSucceeded( Session doc );
  //		public void openFailed();
  //	}
}
final class DocumentHandlerImpl(root: OSCRoot) extends Application.DocumentHandler with OSCRouter {
  import DocumentHandlerImpl._

  private[this] val osc = new OSCRouterWrapper(root, this)

  def getDocuments: Array[Document] = FScape.documentHandler.documents.toArray

  def setActive(doc: Session): Unit = {
    FScape.documentHandler.activeDocument = Some(doc)
    FScape.documentViewHandler.getWindow(doc).foreach(_.front())
  }

  def close(doc: Session): Unit = {
    FScape.documentViewHandler.getWindow(doc).fold {
      doc.dispose()
    } { win =>
      win.dispose()
    }
  }

  def oscGetPathComponent(): String = OSC_DOC

  def oscAddRouter   (subRouter: OSCRouter): Unit = osc.oscAddRouter   (subRouter)
  def oscRemoveRouter(subRouter: OSCRouter): Unit = osc.oscRemoveRouter(subRouter)

  def oscRoute(rom: RoutedOSCMessage): Unit =
    if (rom.hasNext) {
      // special handling here as documents can be accessed with different paths
      oscRouteNext(rom.next())
    } else {
      osc.oscRoute(rom)
    }

  def oscQuery_count(): AnyRef = {
    val i: Int = getDocuments.length
    i.asInstanceOf[AnyRef]
  }

  def oscCmd_new(rom: RoutedOSCMessage): Unit = {
    try {
      val procName = rom.msg.getArg(1).toString
      FScape.newDocument(procName, visible = true)
    }
    catch {
      case _: IndexOutOfBoundsException => OSCRoot.failedArgCount(rom);
    }
  }

  def oscCmd_open(rom: RoutedOSCMessage): Unit = {
    val numCopyArgs = 2 // 'open', path
    var argIdx      = 1

    try {
      val path = rom.msg.getArg(argIdx).toString
      argIdx += 1
      val visible = if (rom.msg.getArgCount > argIdx) {
        rom.msg.getArg(argIdx).asInstanceOf[Number].intValue() != 0
      } else {
        true
      }
      val res = ActionOpen.perform(file(path), visible = visible)
      res match {
        case Success(doc) =>
          rom.tryReplyDone(numCopyArgs, Array[AnyRef](doc.getNodeID.asInstanceOf[AnyRef]))
        case Failure(_) => rom.tryReplyFailed(numCopyArgs);
      }
    }
    catch {
      case _: IndexOutOfBoundsException =>

        OSCRoot.failedArgCount(rom)
        rom.tryReplyFailed(numCopyArgs)

      case _: ClassCastException =>
        OSCRoot.failedArgType(rom, argIdx)
        rom.tryReplyFailed(numCopyArgs)
    }
  }

  private[this] def oscRouteNext(rom0: RoutedOSCMessage): Unit = {
    var rom     = rom0
		try {
			val subPath = rom.getPathComponent

      val doc: Option[Document] = if (subPath == OSC_ACTIVE) {
        FScape.documentHandler.activeDocument
      } else {
        val docs = getDocuments
        if (subPath == OSC_ID) {
          rom = rom.next()
          val id = Integer.parseInt(rom.getPathComponent)
          docs.find(_.getNodeID == id)
        } else if (subPath == OSC_INDEX) {
          rom = rom.next()
          val idx = Integer.parseInt(rom.getPathComponent)
          if (docs.length > idx) {
            Some(docs(idx))
          } else {
            None
          }
        } else {
          throw new IllegalArgumentException(subPath)
        }
      }

      doc match {
        case None => OSCRoot.failed( rom.msg, "Document not found" )
        case Some(d) => d.oscRoute(rom)
			}
		}
		catch {
      case _: IndexOutOfBoundsException | _: NumberFormatException | _: IllegalArgumentException =>
        OSCRoot.failedUnknownPath(rom)
		}
  }
}
