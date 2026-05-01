package com.example.otp;

import com.example.otp.config.DatabaseConnection;

public class TestDB {
    public static void main(String[] args) {
        System.out.println("Проверяем подключение к БД...");
        try {
            var conn = DatabaseConnection.getConnection();
            System.out.println("✅ УСПЕХ! База данных подключена!");
            conn.close();
        } catch (Exception e) {
            System.out.println("❌ ОШИБКА: " + e.getMessage());
            e.printStackTrace();
        }
    }
}