package com.mpjmp.gui.model;

import java.util.Date;
import lombok.Data;

@Data
public class Notification {
    private String id;
    private String fileId;
    private String title;
    private String message;
    private boolean read;
    private Date createdAt;
    
    public Notification(String id, String fileId, String title, String message, boolean read, Date createdAt) {
        this.id = id;
        this.fileId = fileId;
        this.title = title;
        this.message = message;
        this.read = read;
        this.createdAt = createdAt;
    }
} 