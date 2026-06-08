package com.mycompany.ebookshop.controller;

import com.mycompany.ebookshop.MainApp;
import com.mycompany.ebookshop.database.UserDAO;
import com.mycompany.ebookshop.models.User;
import com.mycompany.ebookshop.utils.AlertUtils;
import com.mycompany.ebookshop.utils.SessionManager;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class ProfileController {

    @FXML private Label titleLabel;
    @FXML private Label subtitleLabel;
    @FXML private Label statusLabel;
    @FXML private TextField usernameField;
    @FXML private TextField fullNameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;

    private final UserDAO userDAO = new UserDAO();
    private User currentUser;

    @FXML
    public void initialize() {
        currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser == null) {
            AlertUtils.showWarning("No active user session found.");
            MainApp.changeScene("login.fxml", "eBookShop - Login");
            return;
        }

        if (titleLabel != null) {
            titleLabel.setText("My Profile");
        }
        if (subtitleLabel != null) {
            subtitleLabel.setText("Update your username, display name, and password.");
        }
        if (usernameField != null) {
            usernameField.setText(currentUser.getUsername() != null ? currentUser.getUsername() : "");
        }
        if (fullNameField != null) {
            fullNameField.setText(currentUser.getFullName() != null ? currentUser.getFullName() : "");
        }
        if (emailField != null) {
            emailField.setText(currentUser.getEmail() != null ? currentUser.getEmail() : "");
        }
        if (statusLabel != null) {
            statusLabel.setText("Leave password fields blank to keep the current password.");
        }
    }

    @FXML
    public void handleSave() {
        if (currentUser == null) {
            AlertUtils.showWarning("No active user session found.");
            return;
        }

        String newUsername = usernameField != null && usernameField.getText() != null ? usernameField.getText().trim() : "";
        String newFullName = fullNameField != null && fullNameField.getText() != null ? fullNameField.getText().trim() : "";
        String newPassword = passwordField != null && passwordField.getText() != null ? passwordField.getText() : "";
        String confirmPassword = confirmPasswordField != null && confirmPasswordField.getText() != null ? confirmPasswordField.getText() : "";

        if (newUsername.isBlank()) {
            AlertUtils.showWarning("Username cannot be empty.");
            return;
        }

        if (newPassword.isBlank() ^ confirmPassword.isBlank()) {
            AlertUtils.showWarning("Enter and confirm the new password, or leave both fields blank.");
            return;
        }

        if (!newPassword.isBlank() && !newPassword.equals(confirmPassword)) {
            AlertUtils.showWarning("Passwords do not match.");
            return;
        }

        boolean updated = userDAO.updateUserProfile(
                currentUser,
                newUsername,
                newFullName,
                newPassword.isBlank() ? null : newPassword);

        if (!updated) {
            AlertUtils.showError("Unable to update your profile. The username may already be in use.");
            return;
        }

        currentUser.setUsername(newUsername);
        if (!newFullName.isBlank()) {
            currentUser.setFullName(newFullName);
        }
        if (!newPassword.isBlank()) {
            currentUser.setPassword(newPassword);
        }
        SessionManager.getInstance().setCurrentUser(currentUser);

        if (statusLabel != null) {
            statusLabel.setText("Profile updated successfully.");
        }
        AlertUtils.showInfo("Your profile has been updated.");
    }

    @FXML
    public void handleBack() {
        User user = SessionManager.getInstance().getCurrentUser();
        if (user != null && isAdminUser(user)) {
            MainApp.changeScene("admin.fxml", "eBookShop - Admin Dashboard");
            return;
        }
        MainApp.changeScene("main.fxml", "eBookShop - Book Store");
    }

    private boolean isAdminUser(User user) {
        String username = user.getUsername();
        if (username != null && username.trim().equalsIgnoreCase("admin")) {
            return true;
        }

        String role = user.getRole();
        if (role == null) {
            return false;
        }

        String normalizedRole = role.trim().toLowerCase();
        return normalizedRole.equals("admin") || normalizedRole.equals("administrator") || normalizedRole.startsWith("admin");
    }
}
