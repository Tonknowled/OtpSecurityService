package com.example.otp.dao;

import com.example.otp.config.DatabaseConnection;
import com.example.otp.model.OtpCode;

import java.sql.*;
import java.time.LocalDateTime;

public class OtpCodeDao {

    // Сохранить новый OTP код
    public void save(OtpCode otpCode) throws SQLException {
        String sql = "INSERT INTO otp_codes (user_id, operation_id, code, status, expires_at) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, otpCode.getUserId());
            stmt.setString(2, otpCode.getOperationId());
            stmt.setString(3, otpCode.getCode());
            stmt.setString(4, "ACTIVE");
            stmt.setTimestamp(5, Timestamp.valueOf(otpCode.getExpiresAt()));
            stmt.executeUpdate();
        }
    }

    // Найти ACTIVE код по operationId и коду
    public OtpCode findActiveCode(String operationId, String code) throws SQLException {
        String sql = "SELECT * FROM otp_codes WHERE operation_id = ? AND code = ? AND status = 'ACTIVE' AND expires_at > NOW()";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, operationId);
            stmt.setString(2, code);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapRowToOtpCode(rs);
            }
            return null;
        }
    }

    // Обновить статус кода
    public void updateStatus(int id, String status) throws SQLException {
        String sql = "UPDATE otp_codes SET status = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status);
            stmt.setInt(2, id);
            stmt.executeUpdate();
        }
    }

    // Пометить просроченные коды (вызывается раз в минуту)
    public void expireOldCodes() throws SQLException {
        String sql = "UPDATE otp_codes SET status = 'EXPIRED' WHERE status = 'ACTIVE' AND expires_at < NOW()";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
        }
    }

    // Удалить все OTP коды пользователя
    public void deleteByUserId(int userId) throws SQLException {
        String sql = "DELETE FROM otp_codes WHERE user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.executeUpdate();
        }
    }

    private OtpCode mapRowToOtpCode(ResultSet rs) throws SQLException {
        OtpCode code = new OtpCode();
        code.setId(rs.getInt("id"));
        code.setUserId(rs.getInt("user_id"));
        code.setOperationId(rs.getString("operation_id"));
        code.setCode(rs.getString("code"));
        code.setStatus(rs.getString("status"));
        code.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        code.setExpiresAt(rs.getTimestamp("expires_at").toLocalDateTime());
        return code;
    }
}