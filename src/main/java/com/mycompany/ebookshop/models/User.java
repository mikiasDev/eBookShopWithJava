package com.mycompany.ebookshop.models;

import javafx.beans.property.*;

public class User {
    private final IntegerProperty id = new SimpleIntegerProperty();
    private final StringProperty username = new SimpleStringProperty();
    private final StringProperty email = new SimpleStringProperty();
    private final StringProperty password = new SimpleStringProperty();
    private final StringProperty fullName = new SimpleStringProperty();
    private final StringProperty address = new SimpleStringProperty();
    private final StringProperty phone = new SimpleStringProperty();
    private final StringProperty role = new SimpleStringProperty();
    
    public User() {}
    
    public User(int id, String username, String email, String password, 
                String fullName, String address, String phone, String role) {
        this.id.set(id);
        this.username.set(username);
        this.email.set(email);
        this.password.set(password);
        this.fullName.set(fullName);
        this.address.set(address);
        this.phone.set(phone);
        this.role.set(role);
    }
    
    // Getters
    public int getId() { return id.get(); }
    public String getUsername() { return username.get(); }
    public String getEmail() { return email.get(); }
    public String getPassword() { return password.get(); }
    public String getFullName() { return fullName.get(); }
    public String getAddress() { return address.get(); }
    public String getPhone() { return phone.get(); }
    public String getRole() { return role.get(); }
    
    // Setters
    public void setId(int id) { this.id.set(id); }
    public void setUsername(String username) { this.username.set(username); }
    public void setEmail(String email) { this.email.set(email); }
    public void setPassword(String password) { this.password.set(password); }
    public void setFullName(String fullName) { this.fullName.set(fullName); }
    public void setAddress(String address) { this.address.set(address); }
    public void setPhone(String phone) { this.phone.set(phone); }
    public void setRole(String role) { this.role.set(role); }
    
    // Property getters
    public IntegerProperty idProperty() { return id; }
    public StringProperty usernameProperty() { return username; }
    public StringProperty emailProperty() { return email; }
    public StringProperty fullNameProperty() { return fullName; }
    public StringProperty roleProperty() { return role; }
}