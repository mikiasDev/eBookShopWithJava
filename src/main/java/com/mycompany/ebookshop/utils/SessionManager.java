package com.mycompany.ebookshop.utils;

import com.mycompany.ebookshop.models.Book;
import com.mycompany.ebookshop.models.CartItem;
import com.mycompany.ebookshop.models.User;
import java.util.ArrayList;
import java.util.List;

public class SessionManager {
    private static SessionManager instance;
    private User currentUser;
    private List<CartItem> cart;
    
    private SessionManager() {
        cart = new ArrayList<>();
    }
    
    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }
    
    public User getCurrentUser() { return currentUser; }
    public void setCurrentUser(User user) { this.currentUser = user; }
    
    public List<CartItem> getCart() { return cart; }
    
    public void addToCart(Book book) {
        for (CartItem item : cart) {
            if (item.getBook().getId() == book.getId()) {
                item.setQuantity(item.getQuantity() + 1);
                return;
            }
        }
        cart.add(new CartItem(book, 1));
    }
    
    public void removeFromCart(CartItem item) {
        cart.remove(item);
    }
    
    public void updateCart() {
        // Notify observers
    }
    
    public void clearCart() {
        cart.clear();
    }
    
    public double getCartTotal() {
        return cart.stream().mapToDouble(CartItem::getTotal).sum();
    }
    
    public void clearSession() {
        currentUser = null;
        cart.clear();
    }
}