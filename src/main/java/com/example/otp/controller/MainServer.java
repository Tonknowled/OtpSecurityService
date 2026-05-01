package com.example.otp.controller;

import com.example.otp.dao.UserDao;
import com.example.otp.dao.OtpConfigDao;
import com.example.otp.model.User;
import com.example.otp.service.OtpService;
import com.example.otp.util.PasswordUtil;
import com.example.otp.util.TokenUtil;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.InetSocketAddress;
import java.sql.SQLException;
import java.util.stream.Collectors;

public class MainServer {
    private static final Logger logger = LoggerFactory.getLogger(MainServer.class);
    private static final Gson gson = new Gson();
    private static final UserDao userDao = new UserDao();
    private static final OtpConfigDao configDao = new OtpConfigDao();
    private static final OtpService otpService = new OtpService();

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        server.createContext("/register", new RegisterHandler());
        server.createContext("/login", new LoginHandler());
        server.createContext("/admin/config", new AdminConfigHandler());
        server.createContext("/admin/users", new AdminUsersHandler());
        server.createContext("/user/generate", new GenerateOtpHandler());
        server.createContext("/user/validate", new ValidateOtpHandler());

        server.setExecutor(null);
        server.start();
        logger.info("Сервер запущен на порту 8080");
        System.out.println("Server started on port 8080");
    }

    private static void sendResponse(HttpExchange exchange, int statusCode, String message) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, message.getBytes().length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(message.getBytes());
        }
        logger.info("Ответ: {} - {}", statusCode, message.length() > 100 ? message.substring(0, 100) : message);
    }

    static class RegisterHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            logger.info("Запрос: {} {}", exchange.getRequestMethod(), exchange.getRequestURI());

            if (!"POST".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "Method not allowed");
                return;
            }

            String body = new BufferedReader(new InputStreamReader(exchange.getRequestBody()))
                    .lines().collect(Collectors.joining());
            JsonObject json = gson.fromJson(body, JsonObject.class);

            String login = json.get("login").getAsString();
            String password = json.get("password").getAsString();
            String role = json.has("role") ? json.get("role").getAsString() : "USER";

            logger.info("Регистрация пользователя: {}, роль: {}", login, role);

            try {
                if ("ADMIN".equals(role) && userDao.existsAdmin()) {
                    logger.warn("Попытка создать второго админа");
                    sendResponse(exchange, 403, "Admin already exists");
                    return;
                }

                User user = new User(login, PasswordUtil.hashPassword(password), role);
                userDao.createUser(user);
                logger.info("Пользователь создан: {}", login);
                sendResponse(exchange, 201, "User created");
            } catch (SQLException e) {
                logger.error("Ошибка БД при регистрации: {}", e.getMessage());
                sendResponse(exchange, 500, "Database error: " + e.getMessage());
            }
        }
    }

    static class LoginHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            logger.info("Запрос: {} {}", exchange.getRequestMethod(), exchange.getRequestURI());

            if (!"POST".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "Method not allowed");
                return;
            }

            String body = new BufferedReader(new InputStreamReader(exchange.getRequestBody()))
                    .lines().collect(Collectors.joining());
            JsonObject json = gson.fromJson(body, JsonObject.class);

            String login = json.get("login").getAsString();
            String password = json.get("password").getAsString();

            logger.info("Попытка логина: {}", login);

            try {
                User user = userDao.findByLogin(login);
                if (user == null || !PasswordUtil.verifyPassword(password, user.getPasswordHash())) {
                    logger.warn("Неудачный логин: {}", login);
                    sendResponse(exchange, 401, "Invalid credentials");
                    return;
                }

                String token = TokenUtil.generateToken(user.getLogin(), user.getRole());
                JsonObject response = new JsonObject();
                response.addProperty("token", token);
                response.addProperty("role", user.getRole());
                logger.info("Успешный логин: {}, роль: {}", login, user.getRole());
                sendResponse(exchange, 200, gson.toJson(response));
            } catch (SQLException e) {
                logger.error("Ошибка БД при логине: {}", e.getMessage());
                sendResponse(exchange, 500, "Database error");
            }
        }
    }

    static class AdminConfigHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            logger.info("Запрос: {} {}", exchange.getRequestMethod(), exchange.getRequestURI());

            String token = exchange.getRequestHeaders().getFirst("Authorization");
            if (token == null || !"ADMIN".equals(TokenUtil.getRoleFromToken(token))) {
                logger.warn("Доступ запрещен: нет прав админа");
                sendResponse(exchange, 403, "Forbidden");
                return;
            }

            if (!"PUT".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "Method not allowed");
                return;
            }

            String body = new BufferedReader(new InputStreamReader(exchange.getRequestBody()))
                    .lines().collect(Collectors.joining());
            JsonObject json = gson.fromJson(body, JsonObject.class);

            int lifeSeconds = json.get("lifeSeconds").getAsInt();
            int codeLength = json.get("codeLength").getAsInt();

            logger.info("Обновление конфигурации: жизнь={}с, длина={}", lifeSeconds, codeLength);

            try {
                configDao.updateConfig(lifeSeconds, codeLength);
                logger.info("Конфигурация обновлена");
                sendResponse(exchange, 200, "Config updated");
            } catch (SQLException e) {
                logger.error("Ошибка БД: {}", e.getMessage());
                sendResponse(exchange, 500, "Database error");
            }
        }
    }

    static class AdminUsersHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            logger.info("Запрос: {} {}", exchange.getRequestMethod(), exchange.getRequestURI());

            String token = exchange.getRequestHeaders().getFirst("Authorization");
            if (token == null || !"ADMIN".equals(TokenUtil.getRoleFromToken(token))) {
                logger.warn("Доступ запрещен: нет прав админа");
                sendResponse(exchange, 403, "Forbidden");
                return;
            }

            String method = exchange.getRequestMethod();
            try {
                if ("GET".equals(method)) {
                    var users = userDao.getAllUsers();
                    logger.info("Получен список пользователей: {} записей", users.size());
                    sendResponse(exchange, 200, gson.toJson(users));
                } else if ("DELETE".equals(method)) {
                    String path = exchange.getRequestURI().getPath();
                    String[] parts = path.split("/");
                    int userId = Integer.parseInt(parts[parts.length - 1]);
                    userDao.deleteUser(userId);
                    logger.info("Удален пользователь с ID: {}", userId);
                    sendResponse(exchange, 200, "User deleted");
                } else {
                    sendResponse(exchange, 405, "Method not allowed");
                }
            } catch (SQLException | NumberFormatException e) {
                logger.error("Ошибка: {}", e.getMessage());
                sendResponse(exchange, 500, "Database error");
            }
        }
    }

    static class GenerateOtpHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            logger.info("Запрос: {} {}", exchange.getRequestMethod(), exchange.getRequestURI());

            String token = exchange.getRequestHeaders().getFirst("Authorization");
            if (token == null) {
                logger.warn("Нет токена авторизации");
                sendResponse(exchange, 401, "Unauthorized");
                return;
            }

            String login = TokenUtil.getLoginFromToken(token);
            if (login == null) {
                logger.warn("Невалидный токен");
                sendResponse(exchange, 401, "Invalid token");
                return;
            }

            if (!"POST".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "Method not allowed");
                return;
            }

            String body = new BufferedReader(new InputStreamReader(exchange.getRequestBody()))
                    .lines().collect(Collectors.joining());
            JsonObject json = gson.fromJson(body, JsonObject.class);

            String operationId = json.get("operationId").getAsString();
            String contact = json.get("contact").getAsString();

            logger.info("Пользователь {} генерирует код для операции {}", login, operationId);

            try {
                User user = userDao.findByLogin(login);
                String code = otpService.generateCode(user.getId(), operationId, contact);
                JsonObject response = new JsonObject();
                response.addProperty("message", "Code sent to " + contact);
                response.addProperty("code", code);
                logger.info("Код {} отправлен на {}", code, contact);
                sendResponse(exchange, 200, gson.toJson(response));
            } catch (Exception e) {
                logger.error("Ошибка генерации кода: {}", e.getMessage());
                sendResponse(exchange, 500, "Error: " + e.getMessage());
            }
        }
    }

    static class ValidateOtpHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            logger.info("Запрос: {} {}", exchange.getRequestMethod(), exchange.getRequestURI());

            String token = exchange.getRequestHeaders().getFirst("Authorization");
            if (token == null) {
                logger.warn("Нет токена авторизации");
                sendResponse(exchange, 401, "Unauthorized");
                return;
            }

            String login = TokenUtil.getLoginFromToken(token);
            if (login == null) {
                logger.warn("Невалидный токен");
                sendResponse(exchange, 401, "Invalid token");
                return;
            }

            if (!"POST".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "Method not allowed");
                return;
            }

            String body = new BufferedReader(new InputStreamReader(exchange.getRequestBody()))
                    .lines().collect(Collectors.joining());
            JsonObject json = gson.fromJson(body, JsonObject.class);

            String operationId = json.get("operationId").getAsString();
            String code = json.get("code").getAsString();

            logger.info("Пользователь {} проверяет код {} для операции {}", login, code, operationId);

            try {
                boolean isValid = otpService.validateCode(operationId, code);
                if (isValid) {
                    JsonObject response = new JsonObject();
                    response.addProperty("message", "Code validated successfully");
                    logger.info("Код {} успешно проверен", code);
                    sendResponse(exchange, 200, gson.toJson(response));
                } else {
                    logger.warn("Неверный или просроченный код: {}", code);
                    sendResponse(exchange, 400, "Invalid or expired code");
                }
            } catch (Exception e) {
                logger.error("Ошибка валидации: {}", e.getMessage());
                sendResponse(exchange, 500, "Error: " + e.getMessage());
            }
        }
    }
}