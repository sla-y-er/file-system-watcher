package com.fsw.email;

import java.io.File;

public class EmailSender {
    private String smtpHost;
    private int smtpPort;
    private String senderAddress;
    private String appPassword;

    public void send(String to, File file) {}

    private void buildSession() {}

    private void attachFile(Object msg, File f) {}
}