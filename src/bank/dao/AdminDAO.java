package bank.dao;

import bank.config.DBConnection;
import bank.model.Admin;

import java.sql.*;
import java.util.Optional;

public class AdminDAO {

    public Optional<Admin> findByUsername(String username) throws SQLException {
        String sql = "SELECT * FROM admins WHERE username = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new Admin(
                            rs.getInt("admin_id"),
                            rs.getString("username"),
                            rs.getString("password_hash"),
                            rs.getString("full_name")
                    ));
                }
            }
        }
        return Optional.empty();
    }
}
