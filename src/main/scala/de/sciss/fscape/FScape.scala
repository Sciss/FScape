package de.sciss.fscape

import de.sciss.desktop.impl.{LogWindowImpl, WindowImpl, SwingApplicationImpl}
import de.sciss.desktop._
import scala.swing.{Component, Action, Swing}
import Swing._
import scala.swing.event.Key
import de.sciss.fscape.session.{Session, ModulePanel}
import scala.util.control.NonFatal
import javax.swing.{UIManager, KeyStroke}
import java.awt.{Color, GraphicsEnvironment, Toolkit}
import com.alee.laf.checkbox.WebCheckBoxStyle
import com.alee.laf.progressbar.WebProgressBarStyle
import com.alee.laf.StyleConstants
import de.sciss.fscape.gui.PrefsPanel
import de.sciss.fscape.net.{RoutedOSCMessage, OSCRouter, OSCRouterWrapper, OSCRoot}

object FScape extends SwingApplicationImpl("FScape") {
  App =>

  type Document = Session

  private var osc = null: OSCRouterWrapper

  override protected def init(): Unit = {
    try {
      val web = "com.alee.laf.WebLookAndFeel"
      UIManager.installLookAndFeel("Web Look And Feel", web)
      UIManager.setLookAndFeel(web) // Prefs.lookAndFeel.getOrElse(Prefs.defaultLookAndFeel).getClassName)
    } catch {
      case NonFatal(_) =>
    }
    WebCheckBoxStyle.animated = false
    WebProgressBarStyle.progressTopColor    = Color.gray
    WebProgressBarStyle.progressBottomColor = Color.darkGray
    WebProgressBarStyle.highlightWhite      = new Color(255, 255, 255, 48)
    WebProgressBarStyle.highlightDarkWhite  = new Color(255, 255, 255, 0)
    // StyleConstants.animate = false

    /* val logWin: LogWindowImpl = */ new LogWindowImpl {
      def handler: WindowHandler = App.windowHandler
      placeWindow(this, 0f, 1f, 0)
    }
    System.setErr(Console.err)  // por que?

    // ---- bridge to Java world ----
    import de.sciss.fscape.Application
    Application.userPrefs = de.sciss.desktop.Escape.prefsPeer(userPrefs)
    Application.name      = App.name
    Application.version   = "1.0.1" // XXX TODO - read from BuildInfo
    Application.clipboard = Toolkit.getDefaultToolkit.getSystemClipboard
    Application.documentHandler = new Application.DocumentHandler {
      def getDocuments: Array[Session] = new Array(0) // XXX TODO
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

  //      { M_MOD_BETA },
  //      { new StringItem( "SmpSyn",			MI_SMPSYN ),		null },
  //      { new StringItem( "TransientExtr",	MI_TRANSEXTR ),		null },
  //      { new StringItem( "LPFilter",		MI_LPFILTER ),		null }

  protected lazy val menuFactory: Menu.Root = {
    import Menu._

    val itPrefs = Item.Preferences(App)(ActionPreferences())
    val itQuit  = Item.Quit(App)

    val gFile   = Group("file"  , "File")
      .add(Item("open", ActionOpen))
      .addLine()
      .add(Item("close", proxy("Close" -> (menu1 + Key.W))))
      .add(Item("close-all", actionCloseAll))
      .add(Item("save", proxy("Save" -> (menu1 + Key.S))))
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

    Root().add(gFile).add(gNewModule).add(gEdit)
  }

  private class MainWindow extends WindowImpl {
    def handler: WindowHandler = App.windowHandler

    title = App.name
    size  = (400, 400)
    // placeWindow(this, 0.0f, 0.0f, 0)

    closeOperation = Window.CloseExit
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

  def closeAll(): Unit = {
    println("TODO: close all")
  }

  def newDocument(key: String, text: String): Unit = {
    try {
      val clz       = Class.forName(s"de.sciss.fscape.gui.${key}Dlg")
      val modPanel	= clz.newInstance().asInstanceOf[ModulePanel]
      documentHandler.addDocument(modPanel.getDocument())
      val modWin    = new ModuleWindow(modPanel)
      modWin.front()
    } catch {
      case NonFatal(e) =>
        val dlg = DialogSource.Exception(e -> s"New $text")
        dlg.show(None)
        // GUIUtil.displayError(null, e1, getResourceString("menuNew"));
    }
  }

  private object ActionOpen extends Action("Open...") {
    accelerator = Some(menu1 + Key.O)

    def apply(): Unit = {
      println("TODO: Open")
    }
  }

  private class ActionModule(key: String, text: String, stroke: Option[KeyStroke]) extends Action(text) {
    accelerator = stroke

    def apply(): Unit = newDocument(key = key, text = text)
  }

  private def centerOnScreen(w: Window): Unit = placeWindow(w, 0.5f, 0.5f, 0)

  private def placeWindow(w: Window, horizontal: Float, vertical: Float, padding: Int): Unit = {
    val ge  = GraphicsEnvironment.getLocalGraphicsEnvironment
    val bs  = ge.getMaximumWindowBounds
    val b   = w.size
    val x   = (horizontal * (bs.width  - padding * 2 - b.width )).toInt + bs.x + padding
    val y   = (vertical   * (bs.height - padding * 2 - b.height)).toInt + bs.y + padding
    w.location = (x, y)
  }

  private final class ModuleWindow(panel: ModulePanel) extends WindowImpl {
    def handler: WindowHandler = App.windowHandler

    contents  = Component.wrap(panel)
    title     = panel.getModuleName
    pack()
    // centerOnScreen(this)
    placeWindow(this, 0.5f, 0.333f, 0)
  }

  // ---------------- OSCRouter interface ----------------
    private object OSCRouterImpl extends OSCRouter {
      def oscGetPathComponent(): String = "main"

    def oscRoute(rom: RoutedOSCMessage): Unit = osc.oscRoute(rom)

    def oscAddRouter(subRouter: OSCRouter): Unit =
      osc.oscAddRouter(subRouter)

    def oscRemoveRouter(subRouter: OSCRouter): Unit =
      osc.oscRemoveRouter(subRouter)

    //	public void oscCmd_quit( RoutedOSCMessage rom )
    //	{
    //		try {
    //			if( rom.msg.getArgCount() > 1 ) {
    //				if( ((Number) rom.msg.getArg( 1 )).intValue() != 0 ) {
    //					forceQuit();
    //					return;
    //				}
    //			}
    //			quit();
    //		}
    //		catch( ClassCastException e1 ) {
    //			OSCRoot.failedArgType( rom, 1 );
    //		}
    //	}

    def oscQuery_version(): Any = Application.version
  }
}
