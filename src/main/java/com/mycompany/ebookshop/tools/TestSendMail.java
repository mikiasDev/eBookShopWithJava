package com.mycompany.ebookshop.tools;

import com.mycompany.ebookshop.utils.EmailService;


public class TestSendMail {
    public static void main(String[] args) {
        EmailService es = new EmailService();
        boolean ok = es.sendPasswordResetEmail("test@example.com", "tester", "123456", 15);
        System.out.println("Send result: " + ok);
        
        try { Thread.sleep(800); } catch (InterruptedException ignored) {}
        System.exit(ok ? 0 : 2);
    }
}
