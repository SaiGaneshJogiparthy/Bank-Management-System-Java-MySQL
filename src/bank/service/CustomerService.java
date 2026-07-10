package bank.service;

import bank.dao.AuditLogDAO;
import bank.dao.CustomerDAO;
import bank.model.Customer;

import java.sql.SQLException;
import java.util.Optional;

public class CustomerService {

    private final CustomerDAO customerDAO = new CustomerDAO();
    private final AuditLogDAO auditLogDAO = new AuditLogDAO();

    public boolean isUsernameTaken(String username) throws SQLException {
        return customerDAO.findByUsername(username).isPresent();
    }

    public boolean isEmailTaken(String email) throws SQLException {
        return customerDAO.findByEmail(email).isPresent();
    }

    public boolean updateProfile(Customer customer) throws SQLException {
        boolean updated = customerDAO.updateProfile(customer);
        if (updated) {
            auditLogDAO.log("CUSTOMER", customer.getId(), customer.getUsername(),
                    "UPDATE_PROFILE", "Profile updated");
        }
        return updated;
    }

    public Optional<Customer> getCustomer(int customerId) throws SQLException {
        return customerDAO.findById(customerId);
    }
}
