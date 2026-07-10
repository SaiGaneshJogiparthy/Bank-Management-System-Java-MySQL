package bank.dao;

import bank.config.DBConnection;

import java.sql.*;

public class AuditLogDAO {

    public void log(String userType, Integer userId, String username, String action, String details)
            throws SQLException {
        String sql = "INSERT INTO audit_logs (user_type, user_id, username, action, details) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userType);
            if (userId != null) {
                ps.setInt(2, userId);
            } else {
                ps.setNull(2, Types.INTEGER);
            }
            ps.setString(3, username);
            ps.setString(4, action);
            ps.setString(5, details);
            ps.executeUpdate();
        }
    }

    public void printAll() throws SQLException {
        String sql = "SELECT * FROM audit_logs ORDER BY created_at DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            System.out.printf("%-6s %-10s %-15s %-20s %-30s %-20s%n",
                    "ID", "Type", "Username", "Action", "Details", "Time");
            System.out.println("-".repeat(105));
            while (rs.next()) {
                String details = rs.getString("details");
                if (details != null && details.length() > 28) {
                    details = details.substring(0, 25) + "...";
                }
                System.out.printf("%-6d %-10s %-15s %-20s %-30s %-20s%n",
                        rs.getInt("log_id"),
                        rs.getString("user_type"),
                        rs.getString("username"),
                        rs.getString("action"),
                        details != null ? details : "",
                        rs.getTimestamp("created_at"));
            }
        }
    }
}
