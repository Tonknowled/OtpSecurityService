package com.example.otp.notification;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Properties;

public class EmailService {
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
    private String username;
    private String password;
    private String fromEmail;
    private Session session;
    private boolean enabled = false;

    public EmailService() {
        try {
            Properties props = new Properties();
            InputStream is = getClass().getClassLoader().getResourceAsStream("email.properties");
            if (is == null) {
                logger.warn("Файл email.properties не найден. Email рассылка отключена.");
                return;
            }
            props.load(is);

            this.username = props.getProperty("email.username");
            this.password = props.getProperty("email.password");
            this.fromEmail = props.getProperty("email.from");

            Properties mailProps = new Properties();
            mailProps.put("mail.smtp.host", props.getProperty("mail.smtp.host"));
            mailProps.put("mail.smtp.port", props.getProperty("mail.smtp.port"));
            mailProps.put("mail.smtp.auth", props.getProperty("mail.smtp.auth"));
            mailProps.put("mail.smtp.starttls.enable", props.getProperty("mail.smtp.starttls.enable"));

            this.session = Session.getInstance(mailProps, new Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(username, password);
                }
            });
            this.enabled = true;
            logger.info("Email сервис инициализирован для {}", username);
        } catch (Exception e) {
            logger.error("Ошибка инициализации Email: {}", e.getMessage());
        }
    }

    public void sendCode(String toEmail, String code) {
        if (!enabled) {
            logger.warn("Email сервис не настроен, пропускаем отправку");
            return;
        }

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(fromEmail));
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(toEmail));
            message.setSubject("Your OTP Code");
            message.setText("Your verification code is: " + code);
            Transport.send(message);
            logger.info("Email отправлен на {}", toEmail);
        } catch (MessagingException e) {
            logger.error("Ошибка отправки Email: {}", e.getMessage());
        }
    }
}