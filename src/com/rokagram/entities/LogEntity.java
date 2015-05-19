package com.rokagram.entities;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.appengine.api.datastore.Link;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.annotation.OnSave;
import com.rokagram.backend.TimeFormatUtils;

@Entity
public class LogEntity {

    @Id
    private Long id;

    @Index
    private Date modified;

    @Index
    private String type;

    @Index
    private String deviceId;

    @Index
    private String userId;

    private String location;

    private String userName;

    private String message;
    private Link instaReq;

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setInstaReq(Link instaReq) {
        this.instaReq = instaReq;
    }

    public void setInstaReqFromString(String instaReq) {
        if (instaReq != null) {
            this.instaReq = new Link(instaReq);
        } else {
            this.instaReq = null;
        }
    }

    public Link getInstaReq() {
        return instaReq;
    }

    public boolean isInstaReqApi() {
        boolean ret = false;
        if (this.instaReq != null) {
            ret = this.instaReq.getValue().contains("api.instagram.com");
        }
        return ret;
    }

    // https://api.instagram.com/v1/media/popular?access_token=434730386.b13119e.20b5a4dfa7b64ad8b64578932fe2e79d
    public String getInstaReqShort() {
        String ret = null;
        if (this.instaReq != null) {
            ret = this.instaReq.getValue().replace("https://api.instagram.com/v1/", "")
                    .replaceAll("access_token=[^&]*", "");
            if (ret.endsWith("?")) {
                ret = ret.substring(0, ret.length() - 1);
            }
        }

        return ret;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    @JsonIgnore
    public String getModifiedHuman() {
        return TimeFormatUtils.whenInPastHuman(this.modified);
    }

    public void setModified(Date modified) {
        this.modified = modified;
    }

    public Date getModified() {
        return modified;
    }

    @OnSave
    void updateDate() {
        Date now = new Date();
        this.setModified(now);
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

}
