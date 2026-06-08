package com.mycompany.ebookshop.controller;

import com.mycompany.ebookshop.database.UserDAO;
import com.mycompany.ebookshop.utils.TokenService;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class VerifyController {
    @FXML private TextField tokenField;
    @FXML private Button verifyButton;
    @FXML private Label messageLabel;

    @FXML
    public void handleVerify() {
        String token = tokenField == null ? "" : tokenField.getText().trim();
        if (token.isBlank()) {
            showMessage("Please enter the verification token.");
            return;
        }
        setLoading(true);
        new Thread(() -> {
            TokenService ts = new TokenService();
            int userId = ts.verifyToken(token, "verify");
            if (userId > 0) {
                UserDAO dao = new UserDAO();
                boolean ok = dao.markEmailVerified(userId);
                setLoading(false);
                if (ok) showMessage("Email verified successfully. You can now log in."); else showMessage("Verification succeeded but failed to update account. Check logs.");
            } else {
                setLoading(false);
                showMessage("Invalid or expired token.");
            }
        }).start();
    }

    private void setLoading(boolean on) {
        Runnable r = () -> {
            if (verifyButton != null) verifyButton.setDisable(on);
        };
        if (Platform.isFxApplicationThread()) r.run(); else Platform.runLater(r);
    }

    private void showMessage(String msg) {
        Runnable r = () -> {
            if (messageLabel != null) {
                messageLabel.setText(msg == null ? "" : msg);
                messageLabel.setVisible(msg != null && !msg.isBlank());
                messageLabel.setManaged(msg != null && !msg.isBlank());
            }
        };
        if (Platform.isFxApplicationThread()) r.run(); else Platform.runLater(r);
    }
}
