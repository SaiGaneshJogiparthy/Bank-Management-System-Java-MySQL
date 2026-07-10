package bank.dao;

import bank.config.DBConnection;

import java.sql.*;

public class LoginHistoryDAO {

    public void log(String userType, int userId, String username, String status) throws SQLException {
        String sql = "INSERT INTO login_history (user_type, user_id, username, status) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userType);
            ps.setInt(2, userId);
            ps.setString(3, username);
            ps.setString(4, status);
            ps.executeUpdate();
        }
    }

    public void printAll() throws SQLException {
        String sql = "SELECT * FROM login_history ORDER BY login_time DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            System.out.printf("%-6s %-10s %-8s %-15s %-20s %-8s%n",
                    "ID", "Type", "UserID", "Username", "Time", "Status");
            System.out.println("-".repeat(75));
            while (rs.next()) {
                System.out.printf("%-6d %-10s %-8d %-15s %-20s %-8s%n",
                        rs.getInt("history_id"),
                        rs.getString("user_type"),
                        rs.getInt("user_id"),
                        rs.getString("username"),
                        rs.getTimestamp("login_time"),
                        rs.getString("status"));
            }
        }
    }
}
