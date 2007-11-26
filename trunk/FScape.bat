@echo off
cd "$INSTALL_PATH"
java -Xmx512m -ea -cp "build\FScape.jar;libraries\MRJAdapter.jar;libraries\NetUtil.jar" de.sciss.fscape.Main