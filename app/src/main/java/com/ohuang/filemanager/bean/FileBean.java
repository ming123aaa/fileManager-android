package com.ohuang.filemanager.bean;

public class FileBean {
    private String name;
    private long length;
    private boolean  isFolder;
    public FileBean() {

    }

    public FileBean(String name, long length, boolean isFolder) {
        this.name = name;
        this.length = length;
        this.isFolder = isFolder;
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

    public String getFileName() {
        String[] split = name.split("/");
        if (split.length==0){
            return name;
        }else {
            return split[split.length-1];
        }
    }

    public String getLengthToString() {
        if (length<1024) {
            return length + "B";
        }else if (length<1024*1024){
            return length/(1024*1.0f)+"kB";
        }else if(length<1024*1024*1024){
            return length/(1024*1024*1.0f)+"MB";
        }else {
            return length/(1024*1024*1024*1.0f)+"GB";
        }
    }

    public boolean isFolder() {
        return isFolder;
    }

    public void setFolder(boolean folder) {
        isFolder = folder;
    }

    @Override
    public String toString() {
        return "FileBean{" +
                "name='" + name + '\'' +
                ", length=" + length +
                ", isFolder=" + isFolder +
                '}';
    }
}
