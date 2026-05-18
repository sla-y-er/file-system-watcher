package com.fsw.email;

import java.io.File;

public class EmailSender {

    private String smtpHost;
    private int smtpPort;
    private String senderAddress;
    private String appPassword;
    private String defaultRecipient;

    public EmailSender() {
        this.smtpHost = "smtp.gmail.com";
        this.smtpPort = 587;
        this.senderAddress = "sender@example.com";
        this.appPassword = "";
        this.defaultRecipient = "awafaee@uw.edu";
    }

    public EmailSender(String smtpHost, int smtpPort,
                       String senderAddress, String appPassword) {

        this.smtpHost = smtpHost;
        this.smtpPort = smtpPort;
        this.senderAddress = senderAddress;
        this.appPassword = appPassword;
        this.defaultRecipient = "awafaee@uw.edu";
    }

    public void send(String to, File file) {

        if (to == null || to.isBlank()) {
            to = defaultRecipient;
        }

        if (file == null || !file.exists()) {
            System.out.println("Attachment file does not exist.");
            return;
        }

        buildSession();
        attachFile(null, file);

        System.out.println("Email notification prepared successfully.");
        System.out.println("To: " + to);
        System.out.println("Attachment: " + file.getName());
    }

    private void buildSession() {
        System.out.println("Building mock SMTP session...");
        System.out.println("SMTP Host: " + smtpHost);
        System.out.println("SMTP Port: " + smtpPort);
    }

    private void attachFile(Object msg, File f) {
        System.out.println("Attaching file: " + f.getName());
    }
}