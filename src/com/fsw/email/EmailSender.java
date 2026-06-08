package com.fsw.email;

import java.io.File;

/**
 * Prepares an email with a report attachment.
 *
 * <p><b>Mock implementation.</b> In this release the class assembles the message
 * and reports its actions to the console but does not perform live SMTP delivery.
 * The SMTP fields below describe how a real sender would be configured; wiring up
 * actual delivery (for example, over Gmail with OAuth 2.0) is future work.
 *
 * @author Sudip Chaudhary
 * @author Ali Wafaee
 * @version 1.0
 */
public class EmailSender {

    private String smtpHost;
    private int smtpPort;
    private String senderAddress;
    private String appPassword;
    private String defaultRecipient;

    /** Creates a sender with default (placeholder) SMTP settings for mock use. */
    public EmailSender() {
        this.smtpHost = "smtp.gmail.com";
        this.smtpPort = 587;
        this.senderAddress = "sender@example.com";
        this.appPassword = "";
        this.defaultRecipient = "awafaee@uw.edu";
    }

    /**
     * Creates a sender with explicit SMTP settings.
     *
     * @param smtpHost      the SMTP server host name
     * @param smtpPort      the SMTP server port
     * @param senderAddress the sender's email address
     * @param appPassword   the sender's application password
     */
    public EmailSender(String smtpHost, int smtpPort,
                       String senderAddress, String appPassword) {

        this.smtpHost = smtpHost;
        this.smtpPort = smtpPort;
        this.senderAddress = senderAddress;
        this.appPassword = appPassword;
        this.defaultRecipient = "awafaee@uw.edu";
    }

    /**
     * Prepares an email carrying the given file as an attachment.
     *
     * <p>If {@code to} is {@code null} or blank, the default recipient is used.
     * In mock mode this reports the prepared message to the console rather than
     * sending it.
     *
     * @param to   the recipient address, or {@code null}/blank to use the default
     * @param file the attachment; must exist
     */
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

    /** Simulates building the SMTP session (mock: prints the configuration). */
    private void buildSession() {
        System.out.println("Building mock SMTP session...");
        System.out.println("SMTP Host: " + smtpHost);
        System.out.println("SMTP Port: " + smtpPort);
    }

    /**
     * Simulates attaching a file to a message (mock: prints the file name).
     *
     * @param msg the message to attach to (unused in mock mode)
     * @param f   the file to attach
     */
    private void attachFile(Object msg, File f) {
        System.out.println("Attaching file: " + f.getName());
    }
}
