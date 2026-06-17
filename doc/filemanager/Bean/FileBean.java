package com.example.filemanager.Bean;

public class FileBean {
    private String name;
    private long length;
    private boolean isFolder;
    private long lastModified;

    public FileBean() {
    }

    public FileBean(String name, long length, boolean isFolder, long lastModified) {
        this.name = name;
        this.length = length;
        this.isFolder = isFolder;
        this.lastModified = lastModified;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getLength() {
        return length;
    }

    public void setLength(long length) {
        this.length = length;
    }

    public boolean isFolder() {
        return isFolder;
    }

    public void setFolder(boolean folder) {
        isFolder = folder;
    }

    public long getLastModified() {
        return lastModified;
    }

    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }
}
