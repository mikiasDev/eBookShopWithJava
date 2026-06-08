package com.mycompany.ebookshop.tools;

import com.mycompany.ebookshop.database.UserDAO;
import com.mycompany.ebookshop.models.User;
import com.mycompany.ebookshop.utils.EmailService;
import com.mycompany.ebookshop.utils.TokenService;


public class TestResetEmail {
    public static void main(String[] args) {
        String email = args.length > 0 ? args[0] : "test@example.com";
        
        System.out.println("========== Testing Password Reset Email Send ==========");
        System.out.println("Email: " + email);
        System.out.println();
        
        try {
            
            UserDAO userDAO = new UserDAO();
            User user = userDAO.getUserByEmail(email);
            
            if (user == null) {
                System.out.println("! User not found for email: " + email);
                System.out.println("Please register or use an existing user email.");
                return;
            }
            
            System.out.println("✓ User found: " + user.getUsername() + " (ID: " + user.getId() + ")");
            System.out.println();
            
            
            TokenService tokenService = new TokenService();
            String token = tokenService.createToken(user.getId(), "reset", 15 * 60);
            
            if (token == null) {
                System.out.println("! Failed to create reset token.");
                return;
            }
            
            System.out.println("✓ Reset token created: " + token);
            System.out.println();
            
            
            System.out.println("Attempting to send password reset email via Jakarta Mail...");
            System.out.println();
            
            EmailService emailService = new EmailService();
            boolean sent = emailService.sendPasswordResetEmail(email, user.getUsername(), token, 15);
            
            System.out.println();
            if (sent) {
                System.out.println("✓ Email sent successfully!");
            } else {
                System.out.println("! Email send failed. Check SMTP configuration and logs above.");
            }
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println();
        System.out.println("========== Test Complete ==========");
    }
}
