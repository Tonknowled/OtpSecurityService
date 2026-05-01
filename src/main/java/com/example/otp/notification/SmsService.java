package com.example.otp.notification;

import org.jsmpp.bean.*;
import org.jsmpp.session.BindParameter;
import org.jsmpp.session.SMPPSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

public class SmsService {
    private static final Logger logger = LoggerFactory.getLogger(SmsService.class);
    private String host;
    private int port;
    private String systemId;
    private String password;
    private String systemType;
    private String sourceAddress;
    private boolean enabled = false;

    public SmsService() {
        try {
            Properties props = new Properties();
            InputStream is = getClass().getClassLoader().getResourceAsStream("sms.properties");
            if (is == null) {
                logger.warn("Файл sms.properties не найден. SMS рассылка отключена.");
                return;
            }
            props.load(is);

            this.host = props.getProperty("smpp.host");
            this.port = Integer.parseInt(props.getProperty("smpp.port"));
            this.systemId = props.getProperty("smpp.system_id");
            this.password = props.getProperty("smpp.password");
            this.systemType = props.getProperty("smpp.system_type");
            this.sourceAddress = props.getProperty("smpp.source_addr");
            this.enabled = true;
            logger.info("SMS сервис инициализирован с хостом {}", host);
        } catch (Exception e) {
            logger.error("Ошибка инициализации SMS: {}", e.getMessage());
        }
    }

    public void sendCode(String phoneNumber, String code) {
        if (!enabled) {
            logger.warn("SMS сервис не настроен, пропускаем отправку");
            return;
        }

        SMPPSession session = new SMPPSession();
        try {
            BindParameter bindParameter = new BindParameter(
                    BindType.BIND_TX,
                    systemId,
                    password,
                    systemType,
                    TypeOfNumber.UNKNOWN,
                    NumberingPlanIndicator.UNKNOWN,
                    null
            );

            session.connectAndBind(host, port, bindParameter);

            String message = "Your code: " + code;

            session.submitShortMessage(
                    systemType,
                    TypeOfNumber.UNKNOWN,
                    NumberingPlanIndicator.UNKNOWN,
                    sourceAddress,
                    TypeOfNumber.UNKNOWN,
                    NumberingPlanIndicator.UNKNOWN,
                    phoneNumber,
                    new ESMClass(),
                    (byte) 0,
                    (byte) 1,
                    null,
                    null,
                    new RegisteredDelivery(SMSCDeliveryReceipt.DEFAULT),
                    (byte) 0,
                    new GeneralDataCoding(Alphabet.ALPHA_DEFAULT),
                    (byte) 0,
                    message.getBytes(StandardCharsets.UTF_8)
            );

            logger.info("SMS отправлен на номер {}", phoneNumber);
        } catch (Exception e) {
            logger.error("Ошибка отправки SMS: {}", e.getMessage());
        } finally {
            try {
                session.unbindAndClose();
            } catch (Exception e) {
                logger.error("Ошибка закрытия SMPP сессии: {}", e.getMessage());
            }
        }
    }
}