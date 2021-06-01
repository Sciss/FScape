/*
 *  Application.java
 *  (FScape)
 *
 *  Copyright (c) 2001-2021 Hanns Holger Rutz. All rights reserved.
 *
 *  This software is published under the GNU General Public License v3+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 */

package de.sciss.fscape;

import de.sciss.fscape.session.Session;

import java.awt.datatransfer.Clipboard;
import java.io.File;
import java.util.prefs.Preferences;

// bridge from Scala desktop. this way there is no Java code calling back into Scala
public class Application {
    public static Preferences userPrefs;
    public static String name;
    public static String version;
    public static Clipboard clipboard;
    public static DocumentHandler documentHandler;

    public static interface DocumentHandler {
        public Session[] getDocuments();
        public void setActive(Session doc);
        public void close(Session doc);
    }

    public static File installDir;
}
