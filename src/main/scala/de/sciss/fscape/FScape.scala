package de.sciss.fscape

import de.sciss.desktop.impl.{WindowImpl, SwingApplicationImpl}
import de.sciss.desktop._
import scala.swing.{Component, Action, Swing}
import Swing._
import scala.swing.event.Key
import de.sciss.fscape.session.{Session, ModulePanel}
import scala.util.control.NonFatal
import javax.swing.{UIManager, KeyStroke}
import java.awt.datatransfer.Clipboard
import java.awt.Toolkit

object FScape extends SwingApplicationImpl("FScape") {
  type Document = Session

  override protected def init(): Unit = {
    try {
      val web = "com.alee.laf.WebLookAndFeel"
      UIManager.installLookAndFeel("Web Look And Feel", web)
      UIManager.setLookAndFeel(web) // Prefs.lookAndFeel.getOrElse(Prefs.defaultLookAndFeel).getClassName)
    } catch {
      case NonFatal(_) =>
    }

    // ---- bridge to Java world ----
    import de.sciss.fscape.Application
    Application.userPrefs = de.sciss.desktop.Escape.prefsPeer(userPrefs)
    Application.name      = FScape.name
    Application.version   = "1.0.1" // XXX TODO - read from BuildInfo
    Application.clipboard = Toolkit.getDefaultToolkit.getSystemClipboard
    Application.documentHandler = new Application.DocumentHandler {
      def getDocuments: Array[Session] = new Array(0) // XXX TODO
    }

    val f = new MainWindow
    f.front()
  }

  import KeyStrokes._
  private lazy val modules = Vector(
    ("utilities", "Utilities", Vector(
      ("ChangeGain", "Change Gain"    , menu2 + Key.N),
      ("ChannelMgr", "Channel Manager", menu2 + Key.C)
//      { new StringItem( "Concat",			MI_CONCAT ),		KeyStroke.getKeyStroke( KeyEvent.VK_T, PROC_MODIF ) },
//    { new StringItem( "Splice",			MI_SPLICE ),		null },
//    { new StringItem( "MakeLoop",		MI_MAKELOOP ),		null },
//    { new StringItem( "Statistics",		MI_STATISTICS ),	KeyStroke.getKeyStroke( KeyEvent.VK_I, PROC_MODIF ) },
//    { new StringItem( "UnaryOp",		MI_UNARYOP ),		KeyStroke.getKeyStroke( KeyEvent.VK_1, PROC_MODIF ) },
//    { new StringItem( "BinaryOp",		MI_BINARYOP ),		KeyStroke.getKeyStroke( KeyEvent.VK_2, PROC_MODIF ) },
//    { new StringItem( "AmpShaper",		MI_AMPSHAPER ),		KeyStroke.getKeyStroke( KeyEvent.VK_A, PROC_MODIF ) },
//    { new StringItem( "Resample",		MI_RESAMPLE ),		KeyStroke.getKeyStroke( KeyEvent.VK_P, PROC_MODIF ) }
    )),
    ("spectral", "Spectral Domain", Vector(
      ("FIRDesigner",	"FIR Designer", menu2 + Key.R),
      ("Convolution",	"Convolution" , menu2 + Key.L)
      //    { new StringItem( "ComplexConv",	MI_COMPLEXCONV ),	null },
      //    { new StringItem( "Fourier",		MI_FOURIER ),		KeyStroke.getKeyStroke( KeyEvent.VK_F, PROC_MODIF ) },
      //    { new StringItem( "Wavelet",		MI_TRANSL ),		KeyStroke.getKeyStroke( KeyEvent.VK_W, PROC_MODIF ) },
      //    { new StringItem( "Hilbert",		MI_HILBERT ),		KeyStroke.getKeyStroke( KeyEvent.VK_H, PROC_MODIF ) },
      //    { new StringItem( "Mosaic",			MI_MOSAIC ),		null },
      //    { new StringItem( "SonagramExport",	MI_SONAGRAMEXP ),	null },
      //    { new StringItem( "Bleach",			MI_BLEACH ),		null }
    ))
  )

//  private static final Object[][][] mSub = {
//    {
//    },{
//      { M_MOD_SPECT },
//    },{
//      { M_MOD_BANDS },
//      { new StringItem( "BandSplit",		MI_BANDSPLIT ),		KeyStroke.getKeyStroke( KeyEvent.VK_S, PROC_MODIF ) },
//      { new StringItem( "Chebychev",		MI_CHEBYCHEV ),		null },
//      { new StringItem( "Exciter",		MI_EXCITER ),		null },
//      { new StringItem( "Voocooder",		MI_VOOCOODER ),		KeyStroke.getKeyStroke( KeyEvent.VK_V, PROC_MODIF ) }
//    },{
//      { M_MOD_TIME },
//      { new StringItem( "StepBack",		MI_STEPBACK ),		KeyStroke.getKeyStroke( KeyEvent.VK_B, PROC_MODIF ) },
//      { new StringItem( "Kriechstrom",	MI_KRIECH ),		KeyStroke.getKeyStroke( KeyEvent.VK_K, PROC_MODIF ) },
//      { new StringItem( "SerialKilla",	MI_SERIAL ),		null },
//      { new StringItem( "SeekEnjoy",		MI_SEEKENJOY ),		null },
//      { new StringItem( "Lueckenbuesser", MI_LUECKENBUESSER ),null },
//      { new StringItem( "PearsonPlot",	MI_PEARSONPLOT ),	null },
//      { new StringItem( "Sediment",		MI_SEDIMENT ),		null },
//      { new StringItem( "DrMurke",		MI_DRMURKE),		null }
//    },{
//      { M_MOD_DIST },
//      { new StringItem( "FreqMod",		MI_FREQMOD ),		KeyStroke.getKeyStroke( KeyEvent.VK_M, PROC_MODIF ) },
//      { new StringItem( "Laguerre",		MI_LAGUERRE ),		KeyStroke.getKeyStroke( KeyEvent.VK_G, PROC_MODIF ) },
//      { new StringItem( "Ichneumon",		MI_ICHNEUMON ),		KeyStroke.getKeyStroke( KeyEvent.VK_U, PROC_MODIF ) },
//      { new StringItem( "Needlehole",		MI_NEEDLEHOLE ),	null },
//      { new StringItem( "Rotation",		MI_ROTATION ),		null },
//      { new StringItem( "SeekDestroy",	MI_SEEKDESTROY ),	null },
//      { new StringItem( "BlunderFinger",	MI_BLUNDERFINGER ),	null }
//    },{
//      { M_MOD_MISC },
//      { new StringItem( "Sachensucher",	MI_SACHENSUCHER ),	null },
//      { new StringItem( "Recycle",		MI_RECYCLE ),		null },
//      { new StringItem( "Convert",		MI_CONVERT ),		null },
//      { new StringItem( "CDVerify",		MI_CDVERIFY ),		null },
//      { new StringItem( "SMPTESynth",		MI_SMPTESYNTH ),	null },
//      { new StringItem( "BeltramiDecomposition", MI_BELTRAMI ), null }
//    },{
//      { M_MOD_REST },
//      { new StringItem( "CombFilter",		MI_COMBFLT ),		null },
//      { new StringItem( "Declick",		MI_DECLICK ),		null },
//      { new StringItem( "Schizo",			MI_SCHIZO ),		null }
//    },{
//      { M_MOD_BETA },
//      { new StringItem( "SmpSyn",			MI_SMPSYN ),		null },
//      { new StringItem( "TransientExtr",	MI_TRANSEXTR ),		null },
//      { new StringItem( "LPFilter",		MI_LPFILTER ),		null }
//    },{
//      {},
//      { new StringItem( "Batch",			MI_BATCH ),			KeyStroke.getKeyStroke( KeyEvent.VK_9, PROC_MODIF ) },
//      { new StringItem( "SpectPatch",		MI_SPECTPATCH ),	KeyStroke.getKeyStroke( KeyEvent.VK_3, PROC_MODIF ) }
//    }};

  protected lazy val menuFactory: Menu.Root = {
    import Menu._

    val mFile   = Group("file"  , "File")
    val mModule = Group("module", "New Module")

    modules.foreach { case (key, text, sub) =>
      val g = Group(key, text)
      sub.foreach { case (key1, text1, stroke) =>
        val i = Item(key1, new ActionModule(key = key1, text = text1, stroke = stroke))
        g.add(i)
      }
      mModule.add(g)
    }

    Root().add(mFile).add(mModule)
  }

  private class MainWindow extends WindowImpl {
    def handler: WindowHandler = FScape.windowHandler

    title = FScape.name
    size  = (400, 400)

    closeOperation = Window.CloseExit
  }

  def newDocument(key: String, text: String): Unit = {
    try {
      val clz       = Class.forName(s"de.sciss.fscape.gui.${key}Dlg")
      val modPanel	= clz.newInstance().asInstanceOf[ModulePanel]
      documentHandler.addDocument(modPanel.getDocument())
      val modWin    = new ModuleWindow(modPanel)
      modWin.pack()
      modWin.front()
    } catch {
      case NonFatal(e) =>
        val dlg = DialogSource.Exception(e -> s"New $text")
        dlg.show(None)
        // GUIUtil.displayError(null, e1, getResourceString("menuNew"));
    }
  }

  private class ActionModule(key: String, text: String, stroke: KeyStroke) extends Action(text) {
    accelerator = Some(stroke)

    def apply(): Unit = newDocument(key = key, text = text)
  }

  private final class ModuleWindow(panel: ModulePanel) extends WindowImpl {
    def handler: WindowHandler = FScape.windowHandler

    contents  = Component.wrap(panel)
    title     = panel.getModuleName
  }
}
