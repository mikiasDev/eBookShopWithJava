package com.mycompany.ebookshop.utils;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.AuthenticationFailedException;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.io.File;
import java.io.FileOutputStream;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;


public class EmailService {
    private final Properties config = new Properties();

    public EmailService() {
        loadClasspathProperties("mail.properties");
        loadUserProperties();
        setGmailDefaults();
        applyOverridesFromEnvironment();
    }

    private void setGmailDefaults() {
        
        config.putIfAbsent("mail.smtp.host", "smtp.gmail.com");
        config.putIfAbsent("mail.smtp.port", "587");
        config.putIfAbsent("mail.smtp.auth", "true");
        config.putIfAbsent("mail.smtp.starttls.enable", "true");
        config.putIfAbsent("mail.smtp.ssl.enable", "false");
        config.putIfAbsent("mail.smtp.user", "mikiasmulu29@gmail.com");
        config.putIfAbsent("mail.smtp.password", "vmun qhwo ighg jauq");
        config.putIfAbsent("mail.from", "mikiasmulu29@gmail.com");
    }

    public boolean sendVerificationEmail(String toEmail, String username, String token) {
        if (toEmail == null || toEmail.isBlank()) return false;
        String subject = "eBookShop - Verify your email";
        String intro = "Thank you for registering at eBookShop. To verify your email address, use the token below:";
        return sendTokenEmail(toEmail, username, token, subject, intro);
    }

    public boolean sendPasswordResetEmail(String toEmail, String username, String token, int expiresMinutes) {
        if (toEmail == null || toEmail.isBlank()) return false;
        String subject = "eBookShop - Password reset code";
        String intro = "We received a request to reset your eBookShop password. Use the 6-digit code below to continue. "
            + "This code expires in " + expiresMinutes + " minutes.";
        return sendTokenEmail(toEmail, username, token, subject, intro);
    }

    private boolean sendTokenEmail(String toEmail, String username, String token, String subject, String intro) {
        String from = config.getProperty("mail.from", "no-reply@ebookshop.local");
        StringBuilder body = new StringBuilder();
        if (username != null && !username.isBlank()) body.append("Hello ").append(username).append(",\n\n");
        else body.append("Hello,\n\n");
        body.append(intro).append("\n\n");
        body.append(token).append("\n\n");
        body.append("Open the eBookShop app, go to the account verification or password reset screen, and paste the token.\n\n");
        body.append("If you did not request this email, you can ignore it.\n\n");
        body.append("Regards,\nThe eBookShop team");

        try {
            return sendWithJakarta(from, toEmail, subject, body.toString());
        } catch (AuthenticationFailedException e) {
            System.err.println("Jakarta Mail authentication failed: " + e.getMessage());
            return false;
        } catch (Exception e) {
            System.err.println("Jakarta Mail send failed: " + e.getMessage());
            return false;
        }
    }

    private void loadUserProperties() {
        try {
            Path userCfg = Paths.get(System.getProperty("user.home"), ".ebookshop", "mail.properties");
            if (Files.exists(userCfg)) {
                try (InputStream in = Files.newInputStream(userCfg)) {
                    Properties userProps = new Properties();
                    userProps.load(in);
                    config.putAll(userProps);
                }
            }
        } catch (IOException ignored) {
        }
    }

    private void loadClasspathProperties(String resourceName) {
        try (InputStream in = EmailService.class.getClassLoader().getResourceAsStream(resourceName)) {
            if (in == null) {
                return;
            }

            Properties resourceProps = new Properties();
            resourceProps.load(in);
            config.putAll(resourceProps);
        } catch (IOException ignored) {
        }
    }

    private void applyOverridesFromEnvironment() {
        overrideIfPresent("mail.smtp.host", envOrProperty("MAIL_SMTP_HOST", "mail.smtp.host"));
        overrideIfPresent("mail.smtp.port", envOrProperty("MAIL_SMTP_PORT", "mail.smtp.port"));
        overrideIfPresent("mail.smtp.auth", envOrProperty("MAIL_SMTP_AUTH", "mail.smtp.auth"));
        overrideIfPresent("mail.smtp.starttls.enable", envOrProperty("MAIL_SMTP_STARTTLS_ENABLE", "mail.smtp.starttls.enable"));
        overrideIfPresent("mail.smtp.ssl.enable", envOrProperty("MAIL_SMTP_SSL_ENABLE", "mail.smtp.ssl.enable"));
        overrideIfPresent("mail.smtp.ssl.trust", envOrProperty("MAIL_SMTP_SSL_TRUST", "mail.smtp.ssl.trust"));
        overrideIfPresent("mail.smtp.user", envOrProperty("MAIL_SMTP_USER", "mail.smtp.user"));
        overrideIfPresent("mail.smtp.password", envOrProperty("MAIL_SMTP_PASSWORD", "mail.smtp.password"));
        overrideIfPresent("mail.from", envOrProperty("MAIL_FROM", "mail.from"));
    }

    private String envOrProperty(String envKey, String propertyKey) {
        String envValue = System.getenv(envKey);
        if (envValue != null && !envValue.isBlank()) {
            return envValue.trim();
        }
        String sysValue = System.getProperty(propertyKey);
        return sysValue != null && !sysValue.isBlank() ? sysValue.trim() : null;
    }

    private void overrideIfPresent(String key, String value) {
        if (value != null && !value.isBlank()) {
            config.setProperty(key, value);
        }
    }

    private boolean sendWithJakarta(String from, String to, String subject, String body) throws MessagingException {
        DevSmtpServer.startIfLocalConfig();

        Properties props = new Properties();

        String host = config.getProperty("mail.smtp.host", "").trim();
        String port = config.getProperty("mail.smtp.port", "25");
        String auth = config.getProperty("mail.smtp.auth", "false");
        String starttls = config.getProperty("mail.smtp.starttls.enable", "false");
        String sslEnable = config.getProperty("mail.smtp.ssl.enable", "false");
        String user = config.getProperty("mail.smtp.user", "").trim();
        String pass = normalizeSmtpPassword(config.getProperty("mail.smtp.password", ""));

        
        String devModeDiag = config.getProperty("mail.devMode", "false");
        System.out.println("[EmailService] diag: devMode=" + devModeDiag + " host=" + host + " user=" + user);

        if (host.isEmpty()) {
            throw new MessagingException("mail.smtp.host is not configured");
        }
        if (from == null || from.isBlank()) {
            throw new MessagingException("mail.from is not configured");
        }
        if (Boolean.parseBoolean(auth) && (user.isEmpty() || pass.isEmpty())) {
            return false;
        }

        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", port);
        props.put("mail.smtp.auth", auth);
        props.put("mail.smtp.starttls.enable", starttls);
        props.put("mail.smtp.ssl.enable", sslEnable);

        
        String sslTrust = config.getProperty("mail.smtp.ssl.trust");
        if (sslTrust != null) props.put("mail.smtp.ssl.trust", sslTrust);

        
        String debug = config.getProperty("mail.debug");

        Session session;
        if (Boolean.parseBoolean(auth)) {
            Authenticator a = new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(user, pass);
                }
            };
            session = Session.getInstance(props, a);
            if ("true".equalsIgnoreCase(debug) || "1".equals(debug)) session.setDebug(true);
        } else {
            session = Session.getInstance(props);
            if ("true".equalsIgnoreCase(debug) || "1".equals(debug)) session.setDebug(true);
        }

        MimeMessage msg = new MimeMessage(session);
        msg.setFrom(new InternetAddress(from));
        msg.setRecipient(Message.RecipientType.TO, new InternetAddress(to));
        msg.setSubject(subject);
        msg.setText(body, "utf-8");

        
        String devMode = config.getProperty("mail.devMode", "false");
        if ("true".equalsIgnoreCase(devMode) || "1".equals(devMode)) {
            try {
                File outdir = new File(System.getProperty("user.dir"), "target/email-outbox");
                if (!outdir.exists()) outdir.mkdirs();
                String stamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now()).replaceAll("[:\\\\]", "-");
                String fileName = String.format("%s-to-%s.eml", stamp, to.replaceAll("[^a-zA-Z0-9@._-]", "_"));
                File out = new File(outdir, fileName);
                try (FileOutputStream fos = new FileOutputStream(out)) {
                    msg.writeTo(fos);
                }
                System.out.println("[EmailService] Dev mode: wrote message to " + out.getAbsolutePath());
                return true;
            } catch (Exception ex) {
                System.err.println("[EmailService] Dev mode write failed: " + ex.getMessage());
                
            }
        }

        try {
            Transport.send(msg);
            return true;
        } catch (AuthenticationFailedException authEx) {
            System.err.println("SMTP authentication failed (first attempt): " + authEx.getMessage());
            
            try {
                Properties sslProps = new Properties();
                sslProps.putAll(props);
                sslProps.put("mail.smtp.port", "465");
                sslProps.put("mail.smtp.socketFactory.port", "465");
                sslProps.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
                sslProps.put("mail.smtp.ssl.enable", "true");
                sslProps.put("mail.smtp.starttls.enable", "false");
                
                sslProps.put("mail.smtp.ssl.trust", host);

                Session sslSession;
                if (Boolean.parseBoolean(auth)) {
                    Authenticator a2 = new Authenticator() {
                        @Override
                        protected PasswordAuthentication getPasswordAuthentication() {
                            return new PasswordAuthentication(user, pass);
                        }
                    };
                    sslSession = Session.getInstance(sslProps, a2);
                } else {
                    sslSession = Session.getInstance(sslProps);
                }
                if ("true".equalsIgnoreCase(debug) || "1".equals(debug)) sslSession.setDebug(true);

                MimeMessage sslMsg = new MimeMessage(sslSession);
                sslMsg.setFrom(new InternetAddress(from));
                sslMsg.setRecipient(Message.RecipientType.TO, new InternetAddress(to));
                sslMsg.setSubject(subject);
                sslMsg.setText(body, "utf-8");

                Transport.send(sslMsg);
                return true;
            } catch (Exception sslEx) {
                System.err.println("SMTP SSL fallback failed: " + sslEx.getMessage());
                throw authEx;
            }
        }
    }

    private String normalizeSmtpPassword(String password) {
        if (password == null) {
            return "";
        }
        return password.replaceAll("\\s+", "").trim();
    }
}
