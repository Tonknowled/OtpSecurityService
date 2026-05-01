package com.example.otp.dao;

import com.example.otp.config.DatabaseConnection;
import com.example.otp.model.OtpConfig;

import java.sql.*;

public class OtpConfigDao {

    public OtpConfig getConfig() throws SQLException {
        String sql = "SELECT * FROM otp_config LIMIT 1";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                OtpConfig config = new OtpConfig();
                config.setId(rs.getInt("id"));
                config.setCodeLifeSeconds(rs.getInt("code_life_seconds"));
                config.setCodeLength(rs.getInt("code_length"));
                return config;
            }
            return null;
        }
    }

    public void updateConfig(int codeLifeSeconds, int codeLength) throws SQLException {
        String sql = "UPDATE otp_config SET code_life_seconds = ?, code_length = ? WHERE id = 1";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, codeLifeSeconds);
            stmt.setInt(2, codeLength);
            stmt.executeUpdate();
        }
    }
}