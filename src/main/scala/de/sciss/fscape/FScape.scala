/*
 *  FScape.scala
 *  (FScape)
 *
 *  Copyright (c) 2001-2014 Hanns Holger Rutz. All rights reserved.
 *
 *  This software is published under the GNU General Public License v3+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 */

package de.sciss.fscape

import de.sciss.desktop.impl.{LogWindowImpl, WindowImpl, SwingApplicationImpl}
import de.sciss.desktop._
import scala.swing._
import Swing._
import scala.swing.event.{MouseClicked, Key}
import de.sciss.fscape.session.{Session, ModulePanel}
import scala.util.control.NonFatal
import javax.swing.{ImageIcon, UIManager, KeyStroke}
import java.awt.{Color, GraphicsEnvironment, Toolkit}
import com.alee.laf.checkbox.WebCheckBoxStyle
import com.alee.laf.progressbar.WebProgressBarStyle
import de.sciss.fscape.gui.PrefsPanel
import de.sciss.fscape.net.{RoutedOSCMessage, OSCRouter, OSCRouterWrapper, OSCRoot}
import scala.collection.breakOut
import de.sciss.desktop.Menu
import de.sciss.desktop.Window
import de.sciss.file._
import de.sciss.io.IOUtil
import java.net.URL

object FScape extends SwingApplicationImpl("FScape") {
  App =>

  type Document = Session

  private var osc = null: OSCRouterWrapper

  lazy val version: String = buildInfoString("version")

  private def buildInfoString(key: String): String = try {
    val clazz = Class.forName("de.sciss.fscape.BuildInfo")
    val m     = clazz.getMethod(key)
    m.invoke(null).toString
  } catch {
    case NonFatal(e) => "?"
  }

  override protected def init(): Unit = {
    try {
      val web = "com.alee.laf.WebLookAndFeel"
      UIManager.installLookAndFeel("Web Look And Feel", web)
      UIManager.setLookAndFeel(web) // Prefs.lookAndFeel.getOrElse(Prefs.defaultLookAndFeel).getClassName)
    } catch {
      case NonFatal(_) =>
    }
    WebCheckBoxStyle   .animated            = false
    WebProgressBarStyle.progressTopColor    = Color.lightGray
    WebProgressBarStyle.progressBottomColor = Color.gray
    // XXX TODO: how to really turn of animation?
    WebProgressBarStyle.highlightWhite      = new Color(255, 255, 255, 0) // 48)
    WebProgressBarStyle.highlightDarkWhite  = new Color(255, 255, 255, 0)
    // StyleConstants.animate = false

    /* val logWin: LogWindowImpl = */ new LogWindowImpl {
      def handler: WindowHandler = App.windowHandler
      GUI.placeWindow(this, 0f, 1f, 0)
    }
    System.setErr(Console.err)  // por que?

    // ---- bridge to Java world ----
    import de.sciss.fscape.Application
    Application.userPrefs = de.sciss.desktop.Escape.prefsPeer(userPrefs)
    Application.name      = App.name
    Application.version   = App.version
    Application.clipboard = Toolkit.getDefaultToolkit.getSystemClipboard
    Application.documentHandler = new Application.DocumentHandler {
      def getDocuments: Array[Session] = documentHandler.documents.toArray
    }

    // --- osc ----
    // warning : sequence is crucial
    val oscServer = new OSCRoot(Escape.prefsPeer(userPrefs / OSCRoot.DEFAULT_NODE), 0x4653)
    osc           = new OSCRouterWrapper(oscServer, OSCRouterImpl)

    val f = new MainWindow
    f.front()
  }

  private type ItemConfig   = (String, String, Option[KeyStroke])
  private type GroupConfig  = (String, String, Vector[ItemConfig])

  import KeyStrokes._
  private lazy val modules = Vector[GroupConfig](
    ("utilities", "Utilities", Vector[ItemConfig](
      ("ChangeGain" , "Change Gain"           , Some(menu2 + Key.N)),
      ("ChannelMgr" , "Channel Manager"       , Some(menu2 + Key.C)),
      ( "Concat"    , "Concat"                , Some(menu2 + Key.T)),
      ("Splice"     , "Slice"                 , None               ), // XXX TODO - typo
      ("MakeLoop"   , "Make Loop"             , None               ),
      ("Statistics" , "Statistics"            , Some(menu2 + Key.I)),
      ("UnaryOp"    , "Unary Operator"        , Some(menu2 + Key.Key1)),
      ("BinaryOp"   , "Binary Operator"       , Some(menu2 + Key.Key2)),
      ("AmpShaper"  , "Amplitude Shaper"      , Some(menu2 + Key.A)),
      ("Resample"   , "Resample"              , Some(menu2 + Key.P))
    )),
    ("spectral", "Spectral Domain", Vector[ItemConfig](
      ("FIRDesigner",	"FIR Designer"          , Some(menu2 + Key.R)),
      ("Convolution",	"Convolution"           , Some(menu2 + Key.L)),
      ("ComplexConv",	"Complex Convolution"   , None),
      ("Fourier"    , "Fourier Translation"   , Some(menu2 + Key.F)),
      ("Wavelet"    , "Wavelet Translation"   , Some(menu2 + Key.W)),
      ("Hilbert"    , "Hilbert Filter"        , Some(menu2 + Key.H)),
      ("Mosaic"     , "Mosaic"                , None),
      ("SonagramExport", "Sonogram Export"    , None), // XXX TODO: typo
      ("Bleach"     , "Bleach"                , Some(menu2 + Key.E)),
      ("SpectPatch" ,	"Spectral Patcher"      , Some(menu2 + Key.Key3))
    )),
    ("multi-band", "Multi-band", Vector[ItemConfig](
      ("BandSplit"  ,	"Band Splitting"        , Some(menu2 + Key.S)),
      ("Chebychev"  , "Chebyshev Waveshaping" , None),  // XXX TODO: typo
      ("Exciter"    , "Exciter"               , None),
      ("Voocooder"  , "Voocooder"             , Some(menu2 + Key.V))
    )),
    ("time-domain", "Time Domain", Vector[ItemConfig](
      ("StepBack"   ,	"Step Back"             , Some(menu2 + Key.B)),
      ("Kriechstrom",	"Kriechstrom"           , Some(menu2 + Key.K)),
      ("SerialKilla",	"Serial Killer"         , None),
      ("SeekEnjoy"  ,	"Seek + Enjoy"          , None),
      ("Lueckenbuesser", "Lückenbüßer"        , None),
      ("PearsonPlot",	"Pearson Plotter"       , None),
      ("Sediment"   , "Sediment"              , None),
      ("DrMurke"    , "Dr Murke"              , None)
    )),
    ("distortion", "Distortion", Vector[ItemConfig](
      ("FreqMod"    , "Frequency Modulation"  , Some(menu2 + Key.M)),
      ("Laguerre"   ,	"Laguerre Warping"      , Some(menu2 + Key.G)),
      ("Ichneumon"  ,	"Ichneumon"             , Some(menu2 + Key.U)),
      ("Needlehole" ,	"Needlehole Cherry Blossom", None),
      ("Rotation"   ,	"Rotation"              , None),
      ("SeekDestroy",	"Seek + Destroy"        , None),
      ("BlunderFinger",	"BlunderFinger"       , None)
    )),
    ("misc", "Miscellaneous", Vector[ItemConfig](
      ("Sachensucher",	"Sachensucher"        , None),
      ("Recycle"    ,		"Recycle Or Die"      , None),
      ("Convert"    ,		"Convert Files"       , None),
      ("CDVerify"   ,		"CD Verification"     , None),
      ("SMPTESynth" ,		"SMPTE Synthesizer"   , None),
      ("BeltramiDecomposition", "Beltrami Decomposition", None)
    )),
    ("restauration", "Restauration", Vector[ItemConfig](
      ("CombFilter" ,	"Comb Filter"           , None),
      ("Declick"    ,	"Declick"               , None),
      ("Schizo"     , "Schizophrenia"         , None)
    )),
    ("", "", Vector[ItemConfig](
      ( "Batch"     , "Batch Processor"       , Some(menu2 + Key.Key9))
    ))
  )

  private lazy val moduleNames: Map[String, String] = modules.flatMap { case (_, _, vec) =>
    vec.map { case (key, n, _) => (key, n) }
  } (breakOut)

  def moduleName(key: String): String = moduleNames.getOrElse(key, key)

  //      { M_MOD_BETA },
  //      { new StringItem( "SmpSyn",			MI_SMPSYN ),		null },
  //      { new StringItem( "TransientExtr",	MI_TRANSEXTR ),		null },
  //      { new StringItem( "LPFilter",		MI_LPFILTER ),		null }

  def menuRoot: Menu.Root = menuFactory

  protected lazy val menuFactory: Menu.Root = {
    import Menu._

    val itPrefs = Item.Preferences(App)(ActionPreferences())
    val itQuit  = Item.Quit(App)

    Desktop.addQuitAcceptor(closeAll("Quit"))

    val gFile   = Group("file"  , "File")
      .add(Item("open", ActionOpen))
      .add(ActionOpen.recentMenu)
      .addLine()
      .add(Item("close",   proxy("Close"      -> (menu1         + Key.W))))
      .add(Item("close-all", actionCloseAll))
      .add(Item("save"   , proxy("Save"       -> (menu1         + Key.S))))
      .add(Item("save-as", proxy("Save As..." -> (menu1 + shift + Key.S))))

    if (itQuit.visible) gFile.addLine().add(itQuit)

    val gNewModule = Group("module", "New Module")

    modules.foreach { case (key, text, sub) =>
      val g = if (key == "") gNewModule else {
        val res = Group(key, text)
        gNewModule.add(res)
        res
      }
      sub.foreach { case (key1, text1, stroke) =>
        val i = Item(key1, new ActionModule(key = key1, text = text1, stroke = stroke))
        g.add(i)
      }
    }

    val gEdit = Group("edit", "Edit")
    val keyRedo = if (Desktop.isWindows) menu1 + Key.Y else menu1 + shift + Key.Z
    gEdit
      .add(Item("undo", proxy("Undo" -> (menu1 + Key.Z))))
      .add(Item("redo", proxy("Redo" -> keyRedo)))
    if (itPrefs.visible /* && Desktop.isLinux */) gEdit.addLine().add(itPrefs)

    val gWindow = Group("window", "Window")

    val itAbout = Item.About(App) {
      val addr    = "www.sciss.de/fscape"
      val url     = s"http://$addr/"
      val version = Application.version
      val html =
        s"""<html><center>
           |<font size=+1><b>About ${App.name}</b></font><p>
           |Version $version<p>
           |<p>
           |Copyright (c) 2001&ndash;2014 Hanns Holger Rutz.<p>
           |This software is published under the GNU General Public License v3+<p>
           |<p>
           |<a href="$url">$addr</a>
           |""".stripMargin
      val lb = new Label(html) {
        // cf. http://stackoverflow.com/questions/527719/how-to-add-hyperlink-in-jlabel
        // There is no way to directly register a HyperlinkListener, despite hyper links
        // being rendered... A simple solution is to accept any mouse click on the label
        // to open the corresponding website.
        cursor = java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR)
        listenTo(mouse.clicks)
        reactions += {
          case MouseClicked(_, _, _, 1, false) => Desktop.browseURI(new URL(url).toURI)
        }
      }

      OptionPane.message(message = lb.peer).show(None /* Some(frame) */)
    }

    val gHelp = Group("help", "Help")
    if (itAbout.visible) gHelp.add(itAbout).addLine()
    gHelp.add(Item("index", ActionHelpIndex))

    Root()
      .add(gFile)
      .add(gNewModule)
      .add(gEdit)
      // XXX TODO: .add(gWindow)
      .add(gHelp)
  }

  private class MainWindow extends WindowImpl {
    def handler: WindowHandler = App.windowHandler

    title = App.name
    contents = new BoxPanel(Orientation.Horizontal) {
      background = Color.black
      contents += HGlue
      contents += new Label(null, new ImageIcon(App.getClass.getResource("application.png")), Alignment.Leading)
      contents += HGlue
    }
    resizable = false
    pack() // size  = (400, 400)
    // placeWindow(this, 0.0f, 0.0f, 0)

    closeOperation  = Window.CloseIgnore
    reactions += {
      case Window.Closing(_) => tryQuit()
    }
  }

  private def ActionPreferences(): Unit = {
    val panel = new PrefsPanel
    val opt = OptionPane(message = panel, optionType = OptionPane.Options.OkCancel,
      messageType = OptionPane.Message.Plain, entries = Seq("Close"))
    opt.title = "Preferences"
    opt.show(None)
  }

  private object actionCloseAll extends Action("Close All") {
    accelerator = Some(menu1 + shift + Key.W)
    def apply(): Unit = closeAll()
  }

  def closeAll(title: String = "Close All"): Boolean = documentViewHandler.windows.forall(_.tryClose(title))

  def newDocument(key: String): Option[ModulePanel] = {
    val text = moduleName(key)
    try {
      val clz       = Class.forName(s"de.sciss.fscape.gui.${key}Dlg")
      val modPanel	= clz.newInstance().asInstanceOf[ModulePanel]
      documentHandler.addDocument(modPanel.getDocument())
      val modWin    = new DocumentWindow(modPanel)
      modWin.front()
      Some(modPanel)
    } catch {
      case NonFatal(e) =>
        val dlg = DialogSource.Exception(e -> s"New $text")
        dlg.show(None)
        None
        // GUIUtil.displayError(null, e1, getResourceString("menuNew"));
    }
  }

  private var forcedQuit = false

  private def forceQuit(): Unit = {
    forcedQuit = true
    quit()
  }

  def tryQuit(): Unit = if (Desktop.mayQuit()) quit()

  private class ActionModule(key: String, text: String, stroke: Option[KeyStroke]) extends Action(text) {
    accelerator = stroke

    def apply(): Unit = newDocument(key)
  }

  private object ActionHelpIndex extends Action("Index") {
    def apply(): Unit =
      Desktop.browseURI((file("help") / "index.html").toURI)
  }

  private lazy val docViewH = new DocumentViewHandler

  def documentViewHandler: DocumentViewHandler = docViewH

  // ---------------- OSCRouter interface ----------------
  private object OSCRouterImpl extends OSCRouter {
    def oscGetPathComponent(): String = "main"

    def oscRoute(rom: RoutedOSCMessage): Unit = osc.oscRoute(rom)

    def oscAddRouter(subRouter: OSCRouter): Unit =
      osc.oscAddRouter(subRouter)

    def oscRemoveRouter(subRouter: OSCRouter): Unit =
      osc.oscRemoveRouter(subRouter)

    	def oscCmd_quit(rom: RoutedOSCMessage): Unit = {
    		try {
    			if( rom.msg.getArgCount > 1 ) {
            if (rom.msg.getArg(1).asInstanceOf[Number].intValue() != 0) {
              forceQuit()
              return
            }
          }
    			tryQuit()
    		} catch {
          case e1: ClassCastException => OSCRoot.failedArgType(rom, 1)
    		}
    	}

    def oscQuery_version(): Any = Application.version
  }
}
