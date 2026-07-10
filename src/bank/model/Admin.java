package bank.model;

public class Admin extends Person {

    public Admin() {
    }

    public Admin(int id, String username, String passwordHash, String fullName) {
        super(id, username, passwordHash, fullName);
    }

    @Override
    public String getRole() {
        return "ADMIN";
    }
}
