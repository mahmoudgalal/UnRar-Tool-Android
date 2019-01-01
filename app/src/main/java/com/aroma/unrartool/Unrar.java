/*
 * Copyright (c) 2019.
 * Mahmoud Galal
 *
 */

package com.aroma.unrartool;


import java.util.concurrent.atomic.AtomicBoolean;


public class Unrar {

    //Rar lib error codes,see unrar.dll manual
    public static final int ERAR_END_ARCHIVE = 10;
    public static final int ERAR_NO_MEMORY = 11;
    public static final int ERAR_BAD_DATA = 12;
    public static final int ERAR_BAD_ARCHIVE = 13;
    public static final int ERAR_UNKNOWN_FORMAT = 14;
    public static final int ERAR_EOPEN = 15;
    public static final int ERAR_ECREATE = 16;
    public static final int ERAR_ECLOSE = 17;
    public static final int ERAR_EREAD = 18;
    public static final int ERAR_EWRITE = 19;
    //static final int  ERAR_SMALL_BUF          20
    //static final int  ERAR_UNKNOWN            21
    //static final int  ERAR_MISSING_PASSWORD   22

    private volatile String passWord = null;
    public volatile String archiveComment = null;
    public volatile boolean locked = false;
    public volatile boolean signed = false;
    public volatile boolean recoveryRecord = false;
    public volatile boolean solid = false;
    public volatile boolean commentPresent = false;
    public volatile boolean volume = false;
    public volatile boolean firstVolume = true;
    private AtomicBoolean passSet = new AtomicBoolean(false);

    static {
        System.loadLibrary("unrardyn");
        init();
    }

    /**
     * first call to initialize the unrar lib
     */
    public static native void init();

    public boolean isPassWordSet() {
        return passSet.get();
    }

    public void setPassWord(String pass) {
        passWord = pass;
        passSet.set(true);
    }

    /**
     * Don't call .
     * called from native code for internal purposes.
     * @return
     */
     String getPassWord() {
        if (callListener != null) {
            callListener.onPassWordRequired();
            do {
                //spin wait for thread synchronization purposes
            } while (!passSet.get());
        }
        return passWord;
    }

    private CallBackListener callListener = null;

    /**
     * Opens the supplied Rar archive file and retrieves some meta data of it i.e: comments ,solid...etc
     *
     * @param fname full path of the Rar file
     * @return error code
     */
    public native int RarGetArchiveItems(String fname);

    /**
     * Opens the supplied Rar archive file and extracts it in the passed directory
     *
     * @param filename full path of the Rar file
     * @param extpath  extraction path
     * @return error code
     */
    public native int RarOpenArchive(String filename, String extpath);

    /**
     * unused
     *
     * @param handle
     * @return
     */
    public native int RarCloseArchive(int handle);

    /**
     * Unused
     *
     * @param handle
     * @param DestPath
     * @return
     */
    public native int RarProcessArchive(int handle, String DestPath);

    public void setCallBackListener(CallBackListener listener) {
        callListener = listener;
    }

    /**
     * Called by native code to reflect the current progress in Archive extraction
     *
     * @param messageID
     * @param message   current processed item in the archive
     */
    private void relayMessage(int messageID, String message) {
        if (callListener != null) {

            callListener.onFileProcessed(messageID, message);
        }
    }

    /**
     * used to relay unrar/decompression process progress
     */
    public interface CallBackListener {
        void onFileProcessed(int msgID, String filename);

        /**
         * Called when the archive is encrypted using a password.
         * you should set the password using @setPassWord
         */
        void onPassWordRequired();
    }
}
