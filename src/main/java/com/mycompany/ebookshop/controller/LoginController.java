package com.mycompany.ebookshop.controller;

import com.mycompany.ebookshop.MainApp;
import com.mycompany.ebookshop.database.UserDAO;
import com.mycompany.ebookshop.models.User;
import com.mycompany.ebookshop.utils.AlertUtils;
import com.mycompany.ebookshop.utils.SessionManager;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;

public class LoginController {
    
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    @FXML private Button loginButton;
    @FXML private Button registerButton;
    
    @FXML
    public void initialize() {
        passwordField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                handleLogin();
            }
        });
        
        // Hover effect for register button
        registerButton.setOnMouseEntered(e -> 
            registerButton.setStyle("-fx-background-color: #003399; -fx-text-fill: white; -fx-font-size: 13px; -fx-pref-height: 45; -fx-border-radius: 10; -fx-cursor: hand;")
        );
        registerButton.setOnMouseExited(e -> 
            registerButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #003399; -fx-font-size: 13px; -fx-pref-height: 45; -fx-border-color: #003399; -fx-border-radius: 10; -fx-border-width: 1.5; -fx-cursor: hand;")
        );
    }
    
    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        
        System.out.println("Login attempt - Username: " + username);
        
        if (username.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Please enter both username and password!");
            return;
        }
        
        loginButton.setText("⏳ Logging in...");
        loginButton.setDisable(true);
        registerButton.setDisable(true);
        errorLabel.setText("");
        
        Thread loginThread = new Thread(() -> {
            try {
                Thread.sleep(500);
                UserDAO userDAO = new UserDAO();
                User user = userDAO.authenticate(username, password);
                
                javafx.application.Platform.runLater(() -> {
                    loginButton.setText("LOGIN");
                    loginButton.setDisable(false);
                    registerButton.setDisable(false);
                    
                    if (user != null) {
                        System.out.println("✅ Login successful: " + user.getFullName());
                        SessionManager.getInstance().setCurrentUser(user);
                        AlertUtils.showInfo("Welcome back, " + user.getFullName() + "!");
                        
                        if (user.getRole().equals("admin")) {
                            MainApp.changeScene("admin.fxml", "eBookShop - Admin Panel");
                        } else {
                            MainApp.changeScene("main.fxml", "eBookShop - Book Store");
                        }
                    } else {
                        System.out.println("❌ Login failed for: " + username);
                        errorLabel.setText("Invalid username or password!");
                    }
                });
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    loginButton.setText("LOGIN");
                    loginButton.setDisable(false);
                    registerButton.setDisable(false);
                    errorLabel.setText("Connection error! Please try again.");
                });
            }
        });
        loginThread.setDaemon(true);
        loginThread.start();
    }
    
    @FXML
    private void handleRegister() {
        MainApp.changeScene("register.fxml", "eBookShop - Create Account");
    }
}