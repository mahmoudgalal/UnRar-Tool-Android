/*
 * Copyright (c) 2019.
 * Mahmoud Galal
 *
 */

package com.aroma.unrartool.model;

import java.io.File;

/**
 *   file browser list item
 */
public class FileEntry
{
    public String getAbsolutePath() {
        return absolutePath;
    }

    public void setAbsolutePath(String absolutePath) {
        this.absolutePath = absolutePath;
    }

    public String getFileName() {
        return fileName;
    }

    public boolean isDirectory() {
        return isDirectory;
    }

    public void setDirectory(boolean directory) {
        isDirectory = directory;
    }

    private String absolutePath=null;
    private String fileName=null;
    private File file=null;
    private boolean isDirectory=false;
    public void setFile(File file)
    {
        this.file=file;
        if(file != null)
        {
            absolutePath = file.getAbsolutePath();
            fileName=file.getName();
        }
    }
    public File getFile()
    {
        return file;
    }

}