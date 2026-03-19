package com.vupico.notification.service;

public class RenderedTemplate {

    private final String subject;
    private final String body;

    public RenderedTemplate(String subject, String body) {
        this.subject = subject;
        this.body = body;
    }

    public String getSubject() {
        return subject;
    }

    public String getBody() {
        return body;
    }
}

