package com.mpjmp.gui;

public class FileProgress {
    private String fileName;
    private double progress;

    public FileProgress(String fileName, double progress) {
        this.fileName = fileName;
        this.progress = progress;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public double getProgress() {
        return progress;
    }

    public void setProgress(double progress) {
        this.progress = progress;
    }
}
