package bank.model;

public class Customer extends Person {

    private String email;
    private String transactionPin;
    private String phone;
    private String address;
    private int failedLogins;
    private boolean locked;

    public Customer() {
    }

    public Customer(int id, String username, String passwordHash, String fullName,
                    String email, String transactionPin, String phone, String address,
                    int failedLogins, boolean locked) {
        super(id, username, passwordHash, fullName);
        this.email = email;
        this.transactionPin = transactionPin;
        this.phone = phone;
        this.address = address;
        this.failedLogins = failedLogins;
        this.locked = locked;
    }

    @Override
    public String getRole() {
        return "CUSTOMER";
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTransactionPin() {
        return transactionPin;
    }

    public void setTransactionPin(String transactionPin) {
        this.transactionPin = transactionPin;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public int getFailedLogins() {
        return failedLogins;
    }

    public void setFailedLogins(int failedLogins) {
        this.failedLogins = failedLogins;
    }

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }
}
