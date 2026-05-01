package com.example.otp.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class TelegramService {
    private static final Logger logger = LoggerFactory.getLogger(TelegramService.class);
    private static final String BOT_TOKEN = "8746278301:AAG2M8eVIvSyMqMoxjXSvSGi1F0p2pABVEg";
    private static final String CHAT_ID = "894318549";
    private static final String TELEGRAM_API_URL = "https://api.telegram.org/bot" + BOT_TOKEN + "/sendMessage";

    public TelegramService() {
        logger.info("Telegram сервис инициализирован с chat_id: {}", CHAT_ID);
    }

    public void sendCode(String username, String code) {
        String message = String.format("%s, your confirmation code is: %s", username, code);
        String url = String.format("%s?chat_id=%s&text=%s",
                TELEGRAM_API_URL,
                CHAT_ID,
                urlEncode(message));

        logger.info("Отправка в Telegram. URL: {}", url);
        sendTelegramRequest(url);
    }

    private void sendTelegramRequest(String url) {
        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            logger.info("Telegram API ответ: статус={}, тело={}", response.statusCode(), response.body());
            if (response.statusCode() != 200) {
                logger.error("Telegram API ошибка. Статус: {}, тело: {}", response.statusCode(), response.body());
            } else {
                logger.info("Telegram сообщение отправлено успешно!");
            }
        } catch (InterruptedException e) {
            logger.error("Ошибка отправки Telegram: {}", e.getMessage(), e);
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            logger.error("Ошибка отправки Telegram: {}", e.getMessage(), e);
        }
    }

    private static String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}