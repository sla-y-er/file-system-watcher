package com.fsw.email;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;

import java.io.File;
import java.util.Properties;

public class EmailSender {

    private final String smtpHost;
    private final int smtpPort;
    private final String senderAddress;
    private final String appPassword;

    public EmailSender(
            String smtpHost,
            int smtpPort,
            String senderAddress,
            String appPassword
    ) {

        this.smtpHost = smtpHost;
        this.smtpPort = smtpPort;
        this.senderAddress = senderAddress;
        this.appPassword = appPassword;
    }

    public void send(String to, File file) {

        try {

            Session session = buildSession();

            Message message =
                    new MimeMessage(session);

            message.setFrom(
                    new InternetAddress(
                            senderAddress
                    )
            );

            message.setRecipients(
                    Message.RecipientType.TO,
                    InternetAddress.parse(to)
            );

            message.setSubject(
                    "File Watcher Report"
            );

            MimeBodyPart textPart =
                    new MimeBodyPart();

            textPart.setText(
                    "Attached is your exported "
                            + "file activity report."
            );

            MimeBodyPart filePart =
                    new MimeBodyPart();

            attachFile(filePart, file);

            Multipart multipart =
                    new MimeMultipart();

            multipart.addBodyPart(textPart);
            multipart.addBodyPart(filePart);

            message.setContent(multipart);

            Transport.send(message);

            System.out.println(
                    "Email sent successfully to: "
                            + to
            );

        } catch (Exception e) {

            throw new RuntimeException(
                    "Could not send email: "
                            + e.getMessage(),
                    e
            );
        }
    }

    private Session buildSession() {

        Properties props = new Properties();

        props.put(
                "mail.smtp.auth",
                "true"
        );

        props.put(
                "mail.smtp.starttls.enable",
                "true"
        );

        props.put(
                "mail.smtp.host",
                smtpHost
        );

        props.put(
                "mail.smtp.port",
                String.valueOf(smtpPort)
        );

        return Session.getInstance(
                props,

                new Authenticator() {

                    @Override
                    protected PasswordAuthentication
                    getPasswordAuthentication() {

                        return new PasswordAuthentication(
                                senderAddress,
                                appPassword
                        );
                    }
                }
        );
    }

    private void attachFile(
            MimeBodyPart part,
            File file
    ) {

        try {

            part.attachFile(file);

        } catch (Exception e) {

            throw new RuntimeException(
                    "Could not attach file: "
                            + e.getMessage(),
                    e
            );
        }
    }
}