package com.mycompany.ebookshop.controller;

import com.mycompany.ebookshop.MainApp;
import com.mycompany.ebookshop.database.UserDAO;
import com.mycompany.ebookshop.models.User;
import com.mycompany.ebookshop.utils.GoogleOAuthService;
import com.mycompany.ebookshop.utils.GoogleUserProfile;
import com.mycompany.ebookshop.utils.SessionManager;

import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.util.Duration;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private TextField passwordTextField;
    @FXML private Button togglePasswordBtn;
    @FXML private Text eyeIcon;

    @FXML private HBox errorContainer;
    @FXML private Label errorMsg;
    @FXML private Label errorIcon;

    @FXML private Button loginButton;
    @FXML private ProgressIndicator loadingSpinner;
    @FXML private Button googleSignInButton;
    @FXML private Hyperlink registerLink;
    @FXML private Hyperlink forgotPasswordLink;
    @FXML private VBox cardContainer;

    private volatile boolean isLoggingIn = false;
    private static final String GOOGLE_SIGNIN_ERROR = "Google sign-in is temporarily unavailable. Please login with username/password.";

    @FXML
    public void initialize() {
        
        if (passwordField != null) {
            passwordField.setOnAction(evt -> handleLogin());
        }

        
        if (passwordTextField != null && passwordField != null) {
            passwordTextField.textProperty().bindBidirectional(passwordField.textProperty());
        }

        installHoverAnimation(loginButton);
        installHoverAnimation(googleSignInButton);

        
        if (usernameField != null) {
            usernameField.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene != null && cardContainer != null) {
                    ChangeListener<Number> widthListener = (o, oldW, newW) -> adjustCardWidth(newW.doubleValue());
                    newScene.widthProperty().addListener(widthListener);
                    Platform.runLater(() -> adjustCardWidth(newScene.getWidth()));
                }
            });
        }

        showError("");
        setLoading(false);
    }

    private void adjustCardWidth(double sceneWidth) {
        if (cardContainer == null) return;
        double pref = Math.min(520, Math.max(320, sceneWidth * 0.6));
        cardContainer.setPrefWidth(pref);
    }

    @FXML
    public void handleTogglePassword() {
        if (passwordTextField == null || passwordField == null || eyeIcon == null) return;
        boolean visible = !passwordTextField.isVisible();
        passwordTextField.setVisible(visible);
        passwordTextField.setManaged(visible);
        passwordField.setVisible(!visible);
        passwordField.setManaged(!visible);
        eyeIcon.setText(visible ? "🙈" : "👁");
    }

    @FXML
    public void handleLogin() {
        if (isLoggingIn) return;

        String username = usernameField == null ? "" : usernameField.getText().trim();
        String password = (passwordField != null && passwordField.isVisible()) ? passwordField.getText() : (passwordTextField != null ? passwordTextField.getText() : "");

        if (username.isEmpty() || password.isEmpty()) {
            showError("Please enter username and password.");
            return;
        }

        isLoggingIn = true;
        setLoading(true);
        showError("");

        Thread t = new Thread(() -> performLogin(username, password));
        t.setDaemon(true);
        t.start();
    }

    private void performLogin(String username, String password) {
        ExecutorService ex = Executors.newSingleThreadExecutor();
        try {
            UserDAO dao = new UserDAO();
            Callable<User> task = () -> dao.authenticate(username, password);
            Future<User> future = ex.submit(task);

            User user;
            try {
                user = future.get(5, TimeUnit.SECONDS);
            } catch (TimeoutException te) {
                future.cancel(true);
                Platform.runLater(() -> {
                    setLoading(false);
                    isLoggingIn = false;
                    showError("Login timed out. Check DB connection.");
                });
                return;
            } catch (ExecutionException ee) {
                throw new RuntimeException(ee.getCause());
            }

            final User u = user;
            Platform.runLater(() -> {
                setLoading(false);
                isLoggingIn = false;
                if (u != null) {
                    SessionManager.getInstance().setCurrentUser(u);
                    if (isAdminUser(u)) {
                        MainApp.changeScene("admin.fxml", "eBookShop - Admin");
                    } else {
                        MainApp.changeScene("main.fxml", "eBookShop - Book Store");
                    }
                } else {
                    showError("Invalid username or password.");
                }
            });

        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            Platform.runLater(() -> {
                setLoading(false);
                isLoggingIn = false;
                showError("Login interrupted.");
            });
        } catch (RuntimeException re) {
            Platform.runLater(() -> {
                setLoading(false);
                isLoggingIn = false;
                showError(re.getMessage() == null ? "Connection error. Please check DB." : re.getMessage());
            });
        } finally {
            ex.shutdownNow();
        }
    }

    @FXML
    public void handleGoogleSignIn() {
        showError("");
        setGoogleButtonState(true, "CONNECTING...");

        Thread t = new Thread(() -> {
            try {
                GoogleUserProfile profile = new GoogleOAuthService().authenticate();
                Platform.runLater(() -> setGoogleButtonState(false, "Continue with Google"));

                if (profile == null || profile.getEmail() == null || profile.getEmail().isBlank()) {
                    Platform.runLater(() -> showError(GOOGLE_SIGNIN_ERROR));
                    return;
                }

                if (!profile.isEmailVerified()) {
                    Platform.runLater(() -> showError("Google account email is not verified."));
                    return;
                }

                UserDAO dao = new UserDAO();
                User user = profile.getSubject() == null ? null : dao.getUserByOAuthIdentity("google", profile.getSubject());
                if (user == null) {
                    user = dao.getUserByEmail(profile.getEmail());
                    if (user != null) {
                        dao.linkOAuthIdentity(user.getId(), "google", profile.getSubject());
                        dao.markEmailVerified(user.getId());
                    }
                }
                if (user == null) {
                    user = dao.registerOAuthUser("google", profile.getSubject(), profile.getEmail(), profile.getName());
                }

                final User u = user;
                Platform.runLater(() -> {
                    if (u != null) {
                        SessionManager.getInstance().setCurrentUser(u);
                        if (isAdminUser(u)) {
                            MainApp.changeScene("admin.fxml", "eBookShop - Admin");
                        } else {
                            MainApp.changeScene("main.fxml", "eBookShop - Book Store");
                        }
                    } else {
                        showError(GOOGLE_SIGNIN_ERROR);
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    setGoogleButtonState(false, "Continue with Google");
                    showError(GOOGLE_SIGNIN_ERROR);
                });
            }
        });
        t.setDaemon(true);
        t.start();
    }

    @FXML
    public void handleNavigateToRegister() {
        MainApp.changeScene("register.fxml", "eBookShop - Create Account");
    }

    @FXML
    public void handleForgotPassword() {
        MainApp.changeScene("forgot_password.fxml", "eBookShop - Reset Password");
    }

    private void setGoogleButtonState(boolean disabled, String text) {
        if (googleSignInButton != null) {
            googleSignInButton.setDisable(disabled);
            googleSignInButton.setText(text);
        }
    }

    private void setLoading(boolean loading) {
        if (loginButton != null) loginButton.setDisable(loading);
        if (loadingSpinner != null) {
            loadingSpinner.setVisible(loading);
            loadingSpinner.setManaged(loading);
        }
        if (registerLink != null) registerLink.setDisable(loading);
        if (googleSignInButton != null) googleSignInButton.setDisable(loading);
    }

    private void showError(String message) {
        if (errorContainer == null || errorMsg == null) return;
        boolean show = message != null && !message.isBlank();
        errorContainer.setVisible(show);
        errorContainer.setManaged(show);
        errorMsg.setText(show ? message : "");
    }

    private void installHoverAnimation(Button button) {
        if (button == null) return;
        ScaleTransition in = new ScaleTransition(Duration.millis(110), button);
        in.setToX(1.02); in.setToY(1.02);
        ScaleTransition out = new ScaleTransition(Duration.millis(110), button);
        out.setToX(1); out.setToY(1);
        button.setOnMouseEntered(e -> in.playFromStart());
        button.setOnMouseExited(e -> out.playFromStart());
    }

    private boolean isAdminUser(User u) {
        if (u == null) return false;
        String role = u.getRole();
        if (role != null) {
            String r = role.trim().toLowerCase();
            return r.equals("admin") || r.startsWith("admin") || r.equals("administrator");
        }
        String username = u.getUsername();
        return username != null && username.trim().equalsIgnoreCase("admin");
    }

    private String buildUniqueUsernameSuggestion(UserDAO userDAO, String email) {
        String base = email == null ? "user" : email.split("@",2)[0].replaceAll("[^A-Za-z0-9_]+","_");
        String candidate = base;
        int i = 1;
        while (userDAO.getUserByUsername(candidate) != null && i < 10000) {
            candidate = base + "_" + i;
            i++;
        }
        if (userDAO.getUserByUsername(candidate) != null) candidate = base + "_" + System.nanoTime()%10000;
        return candidate;
    }

}
