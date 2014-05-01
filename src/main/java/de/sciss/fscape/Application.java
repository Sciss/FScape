package de.sciss.fscape;

import de.sciss.fscape.session.Session;

import java.awt.datatransfer.Clipboard;
import java.util.prefs.Preferences;

public class Application {
    public static Preferences userPrefs;
    public static String name;
    public static String version;
    public static Clipboard clipboard;
    public static DocumentHandler documentHandler;

    public static interface DocumentHandler {
        public Session[] getDocuments();
    }
}
