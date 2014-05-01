//package de.sciss.fscape.proc;
//
//import de.sciss.io.AudioFile;
//
//import java.io.EOFException;
//import java.io.IOException;
//
//public abstract class Module {
//    private void notImplemented() {
//        throw new IllegalStateException();
//    }
//
//    protected boolean threadRunning() {
//        notImplemented();
//        return false;
//    }
//
//    protected void errorEmptyFile() throws IOException {
//        throw new EOFException("File is empty");
//    }
//
//    protected void errorOutOfMemory() {
//        throw new RuntimeException("FScape ran out of memory");
//    }
//
//    protected void setProgression(float p) {
//        notImplemented();
//    }
//
//    protected float getProgression() {
//        notImplemented();
//        return 0f;
//    }
//
//    protected void setError(Throwable e) {
//        notImplemented();
//    }
//
//    protected boolean normalizeAudioFile(AudioFile srcF, AudioFile destF, float buf[][], float gain, float progEnd)
//        throws IOException {
//
//        notImplemented();
//        return false;
//    }
//
//    protected void handleClipping(float mxAmp) {
//        notImplemented();
//    }
//}
