package com.mycompany.ebookshop.controller;

import com.mycompany.ebookshop.MainApp;
import com.mycompany.ebookshop.database.UserDAO;
import com.mycompany.ebookshop.models.User;
import com.mycompany.ebookshop.utils.AlertUtils;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.util.regex.Pattern;

public class RegisterController {
    
    @FXML private TextField fullNameField;
    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private TextField addressField;
    @FXML private TextField phoneField;
    @FXML private Label errorLabel;
    @FXML private Label successLabel;
    @FXML private Button registerButton;
    
    private static final Pattern EMAIL_PATTERN = 
        Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
    
    @FXML
    private void handleRegister() {
        errorLabel.setText("");
        successLabel.setText("");
        
        String fullName = fullNameField.getText().trim();
        String username = usernameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();
        String address = addressField.getText().trim();
        String phone = phoneField.getText().trim();
        
        // Validations
        if (fullName.isEmpty()) {
            errorLabel.setText("Please enter your full name!");
            return;
        }
        if (username.isEmpty()) {
            errorLabel.setText("Please choose a username!");
            return;
        }
        if (username.length() < 3) {
            errorLabel.setText("Username must be at least 3 characters!");
            return;
        }
        if (email.isEmpty()) {
            errorLabel.setText("Please enter your email!");
            return;
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            errorLabel.setText("Please enter a valid email address!");
            return;
        }
        if (password.isEmpty()) {
            errorLabel.setText("Please create a password!");
            return;
        }
        if (password.length() < 4) {
            errorLabel.setText("Password must be at least 4 characters!");
            return;
        }
        if (!password.equals(confirmPassword)) {
            errorLabel.setText("Passwords do not match!");
            return;
        }
        
        registerButton.setText("⏳ Registering...");
        registerButton.setDisable(true);
        
        Thread registerThread = new Thread(() -> {
            try {
                Thread.sleep(500);
                UserDAO userDAO = new UserDAO();
                
                User existingUser = userDAO.getUserByUsername(username);
                
                javafx.application.Platform.runLater(() -> {
                    registerButton.setText("REGISTER");
                    registerButton.setDisable(false);
                    
                    if (existingUser != null) {
                        errorLabel.setText("Username already exists! Please choose another.");
                    } else {
                        User newUser = new User(0, username, email, password, 
                                                 fullName, address, phone, "customer");
                        
                        if (userDAO.registerUser(newUser)) {
                            successLabel.setText("✅ Registration successful! Redirecting to login...");
                            clearForm();
                            
                            new Thread(() -> {
                                try {
                                    Thread.sleep(2000);
                                    javafx.application.Platform.runLater(() -> {
                                        AlertUtils.showInfo("Account created successfully! Please login.");
                                        MainApp.changeScene("login.fxml", "eBookShop - Login");
                                    });
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }).start();
                        } else {
                            errorLabel.setText("Registration failed! Please try again.");
                        }
                    }
                });
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    registerButton.setText("REGISTER");
                    registerButton.setDisable(false);
                    errorLabel.setText("Connection error! Please check database.");
                });
            }
        });
        registerThread.setDaemon(true);
        registerThread.start();
    }
    
    @FXML
    private void handleBackToLogin() {
        MainApp.changeScene("login.fxml", "eBookShop - Login");
    }
    
    private void clearForm() {
        fullNameField.clear();
        usernameField.clear();
        emailField.clear();
        passwordField.clear();
        confirmPasswordField.clear();
        addressField.clear();
        phoneField.clear();
    }
}