package com.mycompany.ebookshop.controller;

import java.util.regex.Pattern;

import com.mycompany.ebookshop.MainApp;
import com.mycompany.ebookshop.database.UserDAO;
import com.mycompany.ebookshop.models.User;
import com.mycompany.ebookshop.utils.GoogleOAuthService;
import com.mycompany.ebookshop.utils.GoogleUserProfile;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;

public class RegisterController {

    @FXML private TextField fullNameField;
    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private TextField passwordVisibleField;
    @FXML private TextField confirmPasswordVisibleField;
    @FXML private Hyperlink showPasswordLink;

    @FXML private Label errorLabel;
    @FXML private Label successLabel;
    @FXML private Label strengthLabel;
    @FXML private Label matchLabel;
    @FXML private Label emailErrorLabel;

    @FXML private Button createAccountButton;
    @FXML private ProgressIndicator loadingSpinner;
    @FXML private Hyperlink loginLink;
    @FXML private Button googleSignupButton;

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");

    @FXML
    public void initialize() {
        if (passwordField != null) passwordField.textProperty().addListener((a,b,c) -> { syncVisiblePassword(false); updatePasswordFeedback(); updateActionState(); });
        if (confirmPasswordField != null) confirmPasswordField.textProperty().addListener((a,b,c) -> { syncVisiblePassword(false); updateMatchFeedback(); updateActionState(); });
        if (passwordVisibleField != null) passwordVisibleField.textProperty().addListener((a,b,c) -> { syncVisiblePassword(true); updatePasswordFeedback(); updateMatchFeedback(); updateActionState(); });
        if (confirmPasswordVisibleField != null) confirmPasswordVisibleField.textProperty().addListener((a,b,c) -> { syncVisiblePassword(true); updateMatchFeedback(); updateActionState(); });
        if (emailField != null) emailField.textProperty().addListener((a,b,c) -> updateEmailFeedback());
        if (fullNameField != null) fullNameField.textProperty().addListener((a,b,c) -> updateActionState());
        if (usernameField != null) usernameField.textProperty().addListener((a,b,c) -> updateActionState());
        if (showPasswordLink != null) showPasswordLink.setVisible(true);

        updatePasswordFeedback();
        updateMatchFeedback();
        updateEmailFeedback();
        updateActionState();
    }

    @FXML
    public void handleRegister() {
        showMessage(errorLabel, "");
        showMessage(successLabel, "");

        String fullName = text(fullNameField);
        String username = text(usernameField);
        String email = text(emailField);
        String password = getPasswordText();
        String confirm = getConfirmPasswordText();

        if (fullName.isBlank()) { showMessage(errorLabel, "Please enter your full name."); return; }
        if (username.isBlank() || username.length() < 3) { showMessage(errorLabel, "Please choose a username (3+ chars)."); return; }
        if (email.isBlank() || !EMAIL_PATTERN.matcher(email).matches()) { showMessage(errorLabel, "Please enter a valid email address."); return; }
        if (!isPasswordStrongEnough(password)) { showMessage(errorLabel, "Password too weak. Use min 8 chars with lowercase, uppercase, digit, and special char."); return; }
        if (!password.equals(confirm)) { showMessage(errorLabel, "Passwords do not match."); return; }

        setLoading(true);

        Thread t = new Thread(() -> {
            try {
                UserDAO dao = new UserDAO();
                User u1 = dao.getUserByUsername(username);
                User u2 = dao.getUserByEmail(email);

                Platform.runLater(() -> {
                    if (u1 != null) {
                        setLoading(false);
                        showMessage(errorLabel, "Username already exists.");
                        return;
                    }
                    if (u2 != null) {
                        setLoading(false);
                        showMessage(errorLabel, "Email already registered.");
                        return;
                    }

                    User newUser = new User(0, username, email, password, fullName, null, null, "customer");
                    boolean ok = dao.registerUser(newUser);
                    if (ok) {
                        
                        User stored = dao.getUserByUsername(username);
                        if (stored != null) {
                            
                            com.mycompany.ebookshop.utils.TokenService ts = new com.mycompany.ebookshop.utils.TokenService();
                            String token = ts.createToken(stored.getId(), "verify", 24 * 3600);
                            boolean emailed = new com.mycompany.ebookshop.utils.EmailService().sendVerificationEmail(email, username, token);
                            setLoading(false);
                            if (emailed) {
                                showMessage(successLabel, "Account created. Verification email sent. Check your inbox.");
                            } else {
                                showMessage(successLabel, "Account created. Could not send verification email — check server logs.");
                            }
                        } else {
                            setLoading(false);
                            showMessage(successLabel, "Account created. Redirecting to login...");
                            clearForm();
                            Platform.runLater(() -> {
                                MainApp.changeScene("login.fxml", "eBookShop - Login");
                            });
                        }
                    } else {
                        setLoading(false);
                        showMessage(errorLabel, "Registration failed. Try again later.");
                    }
                });

            } catch (RuntimeException ex) {
                setLoading(false);
                showMessage(errorLabel, "Connection error. Please check your database.");
            }
        });
        t.setDaemon(true);
        t.start();
    }

    @FXML
    public void handleGoogleSignup() {
        showMessage(errorLabel, "");
        showMessage(successLabel, "");
        setLoading(true);

        Thread t = new Thread(() -> {
            try {
                GoogleUserProfile profile = new GoogleOAuthService().authenticate();
                Platform.runLater(() -> setLoading(false));

                if (profile == null || profile.getEmail() == null || profile.getEmail().isBlank()) {
                    showMessage(errorLabel, "Google sign-up did not return a valid email address.");
                    return;
                }

                Platform.runLater(() -> {
                    if (profile.getName() != null && !profile.getName().isBlank() && fullNameField != null) {
                        fullNameField.setText(profile.getName());
                    }
                    if (emailField != null) {
                        emailField.setText(profile.getEmail());
                    }
                    if (usernameField != null && usernameField.getText().isBlank()) {
                        usernameField.setText(buildUsernameSuggestion(profile.getEmail()));
                    }
                    showMessage(successLabel, "Google account verified. Finish the form and register.");
                });
            } catch (Exception ex) {
                javafx.application.Platform.runLater(() -> setLoading(false));
                showMessage(errorLabel, "Google sign-up failed. Please try again.");
            }
        });
        t.setDaemon(true);
        t.start();
    }

    @FXML
    public void handleBackToLogin() {
        MainApp.changeScene("login.fxml", "eBookShop - Login");
    }

    private void setLoading(boolean on) {
        Runnable r = () -> {
            if (createAccountButton != null) createAccountButton.setDisable(on);
            if (loadingSpinner != null) { loadingSpinner.setVisible(on); loadingSpinner.setManaged(on); }
        };
        if (Platform.isFxApplicationThread()) r.run(); else Platform.runLater(r);
    }

    private void updatePasswordFeedback() {
        if (strengthLabel == null) return;
        String password = getPasswordText();
        String s = calculatePasswordStrength(password);
        Runnable r = () -> {
            strengthLabel.setText(password.isBlank() ? "" : ("Strength: " + s));
            strengthLabel.setVisible(!password.isBlank());
            strengthLabel.setManaged(!password.isBlank());
        };
        if (Platform.isFxApplicationThread()) r.run(); else Platform.runLater(r);
    }

    private void updateMatchFeedback() {
        if (matchLabel == null) return;
        String p = getPasswordText();
        String c = getConfirmPasswordText();
        Runnable r = () -> {
            if (p.isBlank() && c.isBlank()) { matchLabel.setText(""); matchLabel.setVisible(false); matchLabel.setManaged(false); return; }
            if (p.equals(c)) { matchLabel.setText("✓ Passwords match"); matchLabel.setVisible(true); matchLabel.setManaged(true); }
            else { matchLabel.setText("✗ Passwords differ"); matchLabel.setVisible(true); matchLabel.setManaged(true); }
        };
        if (Platform.isFxApplicationThread()) r.run(); else Platform.runLater(r);
    }

    private String calculatePasswordStrength(String password) {
        if (password == null || password.isBlank()) return "";
        int score = 0;
        if (password.length() >= 8) score++;
        if (password.matches(".*[a-z].*")) score++;
        if (password.matches(".*[A-Z].*")) score++;
        if (password.matches(".*[0-9].*")) score++;
        if (password.matches(".*[^A-Za-z0-9].*")) score++;
        if (score <= 2) return "Weak";
        if (score <= 4) return "Medium";
        return "Strong";
    }

    private boolean isPasswordStrongEnough(String p) {
        
        return com.mycompany.ebookshop.utils.PasswordUtils.isStrongPassword(p);
    }

    private void updateEmailFeedback() {
        if (emailField == null || emailErrorLabel == null) return;
        String e = emailField.getText().trim();
        if (e.isBlank()) { showMessage(emailErrorLabel, ""); }
        else if (!EMAIL_PATTERN.matcher(e).matches()) { showMessage(emailErrorLabel, "Invalid email address"); }
        else { showMessage(emailErrorLabel, ""); }
    }

    private void updateActionState() {
        if (createAccountButton == null) return;
        String pw = getPasswordText();
        String cpw = getConfirmPasswordText();
        boolean valid = !text(fullNameField).isBlank() && text(usernameField).length() >= 3 && EMAIL_PATTERN.matcher(text(emailField)).matches() && isPasswordStrongEnough(pw) && pw.equals(cpw);
        createAccountButton.setDisable(!valid);
    }

    private String text(TextField f) { return f == null || f.getText() == null ? "" : f.getText().trim(); }

    private String getPasswordText() {
        if (passwordVisibleField != null && passwordVisibleField.isVisible()) return passwordVisibleField.getText() == null ? "" : passwordVisibleField.getText();
        if (passwordField != null) return passwordField.getText() == null ? "" : passwordField.getText();
        return "";
    }

    private String getConfirmPasswordText() {
        if (confirmPasswordVisibleField != null && confirmPasswordVisibleField.isVisible()) return confirmPasswordVisibleField.getText() == null ? "" : confirmPasswordVisibleField.getText();
        if (confirmPasswordField != null) return confirmPasswordField.getText() == null ? "" : confirmPasswordField.getText();
        return "";
    }

    private void showMessage(Label l, String msg) {
        if (l == null) return;
        Runnable r = () -> {
            boolean v = msg != null && !msg.isBlank();
            l.setText(v ? msg : "");
            l.setVisible(v);
            l.setManaged(v);
        };
        if (Platform.isFxApplicationThread()) r.run(); else Platform.runLater(r);
    }

    private void clearForm() {
        if (fullNameField != null) fullNameField.clear();
        if (usernameField != null) usernameField.clear();
        if (emailField != null) emailField.clear();
        if (passwordField != null) passwordField.clear();
        if (confirmPasswordField != null) confirmPasswordField.clear();
        if (passwordVisibleField != null) passwordVisibleField.clear();
        if (confirmPasswordVisibleField != null) confirmPasswordVisibleField.clear();
        updatePasswordFeedback(); updateEmailFeedback(); updateActionState();
    }

    private void syncVisiblePassword(boolean fromVisible) {
        if (passwordField != null && passwordVisibleField != null) {
            if (passwordVisibleField.isVisible()) {
                if (fromVisible) passwordField.setText(passwordVisibleField.getText());
                else passwordVisibleField.setText(passwordField.getText());
            } else {
                passwordVisibleField.setText(passwordField.getText());
            }
        }
        if (confirmPasswordField != null && confirmPasswordVisibleField != null) {
            if (confirmPasswordVisibleField.isVisible()) {
                if (fromVisible) confirmPasswordField.setText(confirmPasswordVisibleField.getText());
                else confirmPasswordVisibleField.setText(confirmPasswordField.getText());
            } else {
                confirmPasswordVisibleField.setText(confirmPasswordField.getText());
            }
        }
    }

    @FXML
    public void handleToggleShowPassword() {
        boolean currentlyVisible = passwordVisibleField != null && passwordVisibleField.isVisible();
        boolean show = !currentlyVisible;
        if (passwordVisibleField != null && passwordField != null) {
            passwordVisibleField.setVisible(show);
            passwordVisibleField.setManaged(show);
            passwordField.setVisible(!show);
            passwordField.setManaged(!show);
            if (show) passwordVisibleField.setText(passwordField.getText()); else passwordField.setText(passwordVisibleField.getText());
        }
        if (confirmPasswordVisibleField != null && confirmPasswordField != null) {
            confirmPasswordVisibleField.setVisible(show);
            confirmPasswordVisibleField.setManaged(show);
            confirmPasswordField.setVisible(!show);
            confirmPasswordField.setManaged(!show);
            if (show) confirmPasswordVisibleField.setText(confirmPasswordField.getText()); else confirmPasswordField.setText(confirmPasswordVisibleField.getText());
        }
        if (showPasswordLink != null) showPasswordLink.setText(show ? "Hide password" : "Show password");
    }

    private String buildUsernameSuggestion(String email) {
        if (email == null || email.isBlank()) {
            return "google_user";
        }

        String baseName = email.split("@", 2)[0].replaceAll("[^A-Za-z0-9_]+", "");
        if (baseName.isBlank()) {
            baseName = "google_user";
        }
        return baseName.length() > 3 ? baseName : baseName + "_user";
    }

}
