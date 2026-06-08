package com.mycompany.ebookshop.controller;

import com.mycompany.ebookshop.MainApp;
import com.mycompany.ebookshop.database.UserDAO;
import com.mycompany.ebookshop.utils.AlertUtils;
import com.mycompany.ebookshop.utils.EmailService;
import com.mycompany.ebookshop.utils.TokenService;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class ForgotPasswordController {

    @FXML private TextField emailField;
    @FXML private TextField tokenField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label errorLabel;
    @FXML private Label successLabel;
    @FXML private Button requestTokenButton;
    @FXML private Button resetButton;

    @FXML
    public void initialize() {
        
        if (emailField != null) {
            emailField.textProperty().addListener((obs, oldV, newV) -> {
                boolean empty = newV == null || newV.trim().isEmpty();
                requestTokenButton.setDisable(empty);
            });
        }

        
        if (tokenField != null) {
            tokenField.textProperty().addListener((obs, oldV, newV) -> {
                if (newV == null) return;
                String digits = newV.replaceAll("\\D", "");
                if (digits.length() > 6) digits = digits.substring(0, 6);
                if (!digits.equals(newV)) {
                    final String updated = digits;
                    Platform.runLater(() -> tokenField.setText(updated));
                    return;
                }
                updateResetButtonState();
            });
        }

        
        if (newPasswordField != null) {
            newPasswordField.textProperty().addListener((obs, o, n) -> updateResetButtonState());
        }
        if (confirmPasswordField != null) {
            confirmPasswordField.textProperty().addListener((obs, o, n) -> updateResetButtonState());
        }

        
        if (requestTokenButton != null) requestTokenButton.setDisable(true);
        if (resetButton != null) resetButton.setDisable(true);
    }

    private void updateResetButtonState() {
        String token = tokenField == null ? "" : tokenField.getText();
        String p1 = newPasswordField == null ? "" : newPasswordField.getText();
        String p2 = confirmPasswordField == null ? "" : confirmPasswordField.getText();

        boolean tokenOk = token != null && token.length() == 6;
        boolean passwordsOk = p1 != null && !p1.isEmpty() && p1.equals(p2);

        resetButton.setDisable(!(tokenOk && passwordsOk));
    }

    @FXML
    public void handleRequestResetToken() {
        errorLabel.setText("");
        successLabel.setText("");

        String email = emailField.getText().trim();
        if (email.isEmpty()) {
            errorLabel.setText("Please enter your email address.");
            return;
        }

        setLoading(true);

        Thread resetThread = new Thread(() -> {
            try {
                UserDAO userDAO = new UserDAO();
                var user = userDAO.getUserByEmail(email);
                if (user == null) {
                    Platform.runLater(() -> {
                        setLoading(false);
                        successLabel.setText("If an account exists for that email, a reset token has been sent.");
                        AlertUtils.showInfo("If an account exists for that email, a reset token has been sent.");
                    });
                    return;
                }

                TokenService tokenService = new TokenService();
                String token = tokenService.createToken(user.getId(), "reset", 15 * 60); 
                EmailService emailService = new EmailService();
                boolean emailed = emailService.sendPasswordResetEmail(email, user.getUsername(), token, 15);

                Platform.runLater(() -> {
                    setLoading(false);
                    if (emailed) {
                        successLabel.setText("A 6-digit reset code was sent. It expires in 15 minutes.");
                        AlertUtils.showInfo("Reset token sent to your email address.");
                    } else {
                        errorLabel.setText("Email could not be sent. Configure a real SMTP server in ~/.ebookshop/mail.properties.");
                        AlertUtils.showWarning("Reset code was created, but SMTP delivery failed. Check your mail configuration and try again.");
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    setLoading(false);
                    errorLabel.setText("Unable to send the reset email right now. Please try again.");
                });
            }
        });
        resetThread.setDaemon(true);
        resetThread.start();
    }

    @FXML
    public void handleResetPassword() {
        errorLabel.setText("");
        successLabel.setText("");

        String token = tokenField.getText().trim();
        String newPassword = newPasswordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        if (token.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
            errorLabel.setText("Please complete every field to reset your password.");
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            errorLabel.setText("The new passwords do not match.");
            return;
        }

        resetButton.setDisable(true);
        resetButton.setText("RESETTING...");

        Thread resetThread = new Thread(() -> {
            try {
                TokenService tokenService = new TokenService();
                int userId = tokenService.verifyToken(token, "reset");
                String errorMessage;
                if (userId > 0) {
                    errorMessage = new UserDAO().resetPasswordByUserId(userId, newPassword);
                } else {
                    errorMessage = "Invalid or expired token.";
                }

                Platform.runLater(() -> {
                    resetButton.setDisable(false);
                    resetButton.setText("RESET PASSWORD");

                    if (errorMessage == null) {
                        successLabel.setText("Password reset successfully. You can now log in.");
                        AlertUtils.showInfo("Your password has been updated.");
                        MainApp.changeScene("login.fxml", "eBookShop - Login");
                    } else {
                        errorLabel.setText(errorMessage);
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    resetButton.setDisable(false);
                    resetButton.setText("RESET PASSWORD");
                    errorLabel.setText("Unable to reset password right now. Please try again.");
                });
            }
        });
        resetThread.setDaemon(true);
        resetThread.start();
    }

    @FXML
    public void handleBackToLogin() {
        MainApp.changeScene("login.fxml", "eBookShop - Login");
    }

    private void setLoading(boolean loading) {
        Runnable r = () -> {
            if (requestTokenButton != null) {
                requestTokenButton.setDisable(loading);
            }
            if (resetButton != null) {
                resetButton.setDisable(loading);
            }
        };
        if (Platform.isFxApplicationThread()) {
            r.run();
        } else {
            Platform.runLater(r);
        }
    }
}
