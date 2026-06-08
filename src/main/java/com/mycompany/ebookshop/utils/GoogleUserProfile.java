package com.mycompany.ebookshop.utils;

public class GoogleUserProfile {
    private final String name;
    private final String email;
    private final String subject;
    private final boolean emailVerified;

    public GoogleUserProfile(String name, String email, String subject, boolean emailVerified) {
        this.name = name;
        this.email = email;
        this.subject = subject;
        this.emailVerified = emailVerified;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getSubject() {
        return subject;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }
}