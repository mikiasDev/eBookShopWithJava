package com.mycompany.ebookshop.models;

import javafx.beans.property.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Order {
    private final IntegerProperty id = new SimpleIntegerProperty();
    private final IntegerProperty userId = new SimpleIntegerProperty();
    private final ObjectProperty<LocalDateTime> orderDate = new SimpleObjectProperty<>();
    private final DoubleProperty totalAmount = new SimpleDoubleProperty();
    private final StringProperty status = new SimpleStringProperty();
    private List<OrderItem> items = new ArrayList<>();
    
    public Order() {}
    
    public Order(int id, int userId, LocalDateTime orderDate, double totalAmount, String status) {
        this.id.set(id);
        this.userId.set(userId);
        this.orderDate.set(orderDate);
        this.totalAmount.set(totalAmount);
        this.status.set(status);
    }
    
    // Getters and Setters
    public int getId() { return id.get(); }
    public void setId(int id) { this.id.set(id); }
    public int getUserId() { return userId.get(); }
    public void setUserId(int userId) { this.userId.set(userId); }
    public LocalDateTime getOrderDate() { return orderDate.get(); }
    public void setOrderDate(LocalDateTime orderDate) { this.orderDate.set(orderDate); }
    public double getTotalAmount() { return totalAmount.get(); }
    public void setTotalAmount(double totalAmount) { this.totalAmount.set(totalAmount); }
    public String getStatus() { return status.get(); }
    public void setStatus(String status) { this.status.set(status); }
    public List<OrderItem> getItems() { return items; }
    public void setItems(List<OrderItem> items) { this.items = items; }
}