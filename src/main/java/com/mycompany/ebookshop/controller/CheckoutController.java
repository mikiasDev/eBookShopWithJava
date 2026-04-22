package com.mycompany.ebookshop.controller;

import com.mycompany.ebookshop.MainApp;
import com.mycompany.ebookshop.database.OrderDAO;
import com.mycompany.ebookshop.models.Order;
import com.mycompany.ebookshop.models.OrderItem;
import com.mycompany.ebookshop.utils.AlertUtils;
import com.mycompany.ebookshop.utils.SessionManager;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.time.LocalDateTime;

public class CheckoutController {
    
    @FXML private TextField fullNameField;
    @FXML private TextField addressField;
    @FXML private TextField phoneField;
    @FXML private TextField emailField;
    @FXML private TextField cardNumberField;
    @FXML private TextField expiryField;
    @FXML private TextField cvvField;
    @FXML private Label subtotalLabel;
    @FXML private Label taxLabel;
    @FXML private Label totalLabel;
    @FXML private ComboBox<String> paymentMethod;
    
    @FXML
    public void initialize() {
        paymentMethod.getItems().addAll("Credit Card", "Debit Card", "PayPal", "Cash on Delivery");
        paymentMethod.setValue("Credit Card");
        updateTotals();
        loadUserInfo();
    }
    
    private void updateTotals() {
        double subtotal = SessionManager.getInstance().getCartTotal();
        double tax = subtotal * 0.08;
        double total = subtotal + tax;
        
        subtotalLabel.setText(String.format("$%.2f", subtotal));
        taxLabel.setText(String.format("$%.2f", tax));
        totalLabel.setText(String.format("$%.2f", total));
    }
    
    private void loadUserInfo() {
        if (SessionManager.getInstance().getCurrentUser() != null) {
            var user = SessionManager.getInstance().getCurrentUser();
            fullNameField.setText(user.getFullName());
            emailField.setText(user.getEmail());
            addressField.setText(user.getAddress());
            phoneField.setText(user.getPhone());
        }
    }
    
    @FXML
    private void handlePlaceOrder() {
        if (validateFields()) {
            Order order = new Order();
            order.setUserId(SessionManager.getInstance().getCurrentUser().getId());
            order.setOrderDate(LocalDateTime.now());
            order.setTotalAmount(SessionManager.getInstance().getCartTotal() * 1.08);
            order.setStatus("Pending");
            
            OrderDAO orderDAO = new OrderDAO();
            int orderId = orderDAO.createOrder(order);
            
            if (orderId > 0) {
                for (var item : SessionManager.getInstance().getCart()) {
                    OrderItem orderItem = new OrderItem(0, orderId, 
                        item.getBook().getId(), 
                        item.getBook().getTitle(),
                        item.getQuantity(), 
                        item.getBook().getPrice());
                    orderDAO.addOrderItem(orderItem);
                }
                
                SessionManager.getInstance().clearCart();
                AlertUtils.showInfo("Order placed successfully! Order ID: " + orderId);
                MainApp.changeScene("main.fxml", "eBookShop - Book Store");
            }
        }
    }
    
    private boolean validateFields() {
        if (fullNameField.getText().isEmpty()) {
            AlertUtils.showWarning("Please enter full name!");
            return false;
        }
        if (addressField.getText().isEmpty()) {
            AlertUtils.showWarning("Please enter address!");
            return false;
        }
        if (!paymentMethod.getValue().equals("Cash on Delivery")) {
            if (cardNumberField.getText().length() < 16) {
                AlertUtils.showWarning("Please enter valid card number!");
                return false;
            }
        }
        return true;
    }
    
    @FXML
    private void handleBackToCart() {
        MainApp.changeScene("cart.fxml", "eBookShop - Shopping Cart");
    }
}