package com.example.otp.model;

import java.time.LocalDateTime;

public class User {
    private int id;
    private String login;
    private String passwordHash;
    private String role; // "ADMIN" или "USER"
    private LocalDateTime createdAt;

    // Пустой конструктор (нужен для JDBC)
    public User() {}

    // Конструктор с полями
    public User(String login, String passwordHash, String role) {
        this.login = login;
        this.passwordHash = passwordHash;
        this.role = role;
    }

    // Геттеры и сеттеры (создай все! Alt+Insert -> Getter and Setter)
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getLogin() { return login; }
    public void setLogin(String login) { this.login = login; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}