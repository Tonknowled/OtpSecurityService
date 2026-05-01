package com.example.otp.service;

import com.example.otp.dao.OtpCodeDao;
import com.example.otp.dao.OtpConfigDao;
import com.example.otp.model.OtpCode;
import com.example.otp.model.OtpConfig;
import com.example.otp.notification.EmailService;
import com.example.otp.notification.SmsService;
import com.example.otp.notification.TelegramService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class OtpService {
    private static final Logger logger = LoggerFactory.getLogger(OtpService.class);
    private final OtpCodeDao otpCodeDao = new OtpCodeDao();
    private final OtpConfigDao configDao = new OtpConfigDao();
    private final EmailService emailService = new EmailService();
    private final SmsService smsService = new SmsService();
    private final TelegramService telegramService = new TelegramService();
    private final SecureRandom random = new SecureRandom();

    public OtpService() {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            try {
                otpCodeDao.expireOldCodes();
                logger.debug("Проверка просроченных кодов выполнена");
            } catch (Exception e) {
                logger.error("Ошибка при проверке просроченных кодов: {}", e.getMessage());
            }
        }, 0, 1, TimeUnit.MINUTES);
        logger.info("OtpService инициализирован, фоновый поток запущен");
    }

    public String generateCode(int userId, String operationId, String contact) throws Exception {
        OtpConfig config = configDao.getConfig();
        int length = config.getCodeLength();
        int lifeSeconds = config.getCodeLifeSeconds();

        StringBuilder code = new StringBuilder();
        for (int i = 0; i < length; i++) {
            code.append(random.nextInt(10));
        }

        OtpCode otpCode = new OtpCode();
        otpCode.setUserId(userId);
        otpCode.setOperationId(operationId);
        otpCode.setCode(code.toString());
        otpCode.setExpiresAt(LocalDateTime.now().plusSeconds(lifeSeconds));

        otpCodeDao.save(otpCode);
        logger.info("Создан код {} для операции {} (userId={})", code, operationId, userId);

        // Отправка по всем каналам
        emailService.sendCode(contact, code.toString());
        smsService.sendCode(contact, code.toString());
        telegramService.sendCode("User " + userId, code.toString());

        saveCodeToFile(operationId, code.toString());

        return code.toString();
    }

    public boolean validateCode(String operationId, String inputCode) throws Exception {
        OtpCode otpCode = otpCodeDao.findActiveCode(operationId, inputCode);
        if (otpCode == null) {
            logger.warn("Неверный или просроченный код для операции {}: {}", operationId, inputCode);
            return false;
        }
        otpCodeDao.updateStatus(otpCode.getId(), "USED");
        logger.info("Код {} успешно проверен для операции {}", inputCode, operationId);
        return true;
    }

    private void saveCodeToFile(String operationId, String code) {
        try {
            java.nio.file.Files.writeString(
                    java.nio.file.Path.of(operationId + "_code.txt"),
                    "Code: " + code + "\nCreated: " + LocalDateTime.now()
            );
            logger.info("Код сохранен в файл: {}.txt", operationId);
        } catch (Exception e) {
            logger.error("Ошибка сохранения в файл: {}", e.getMessage());
        }
    }
}