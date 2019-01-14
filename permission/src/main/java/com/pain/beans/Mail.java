package com.pain.beans;

import java.util.Set;

/**
 * Created by Administrator on 2018/6/14.
 */
public class Mail {
    private String subject;

    private String message;

    private Set<String> receivers;

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Set<String> getReceivers() {
        return receivers;
    }

    public void setReceivers(Set<String> receivers) {
        this.receivers = receivers;
    }
}
