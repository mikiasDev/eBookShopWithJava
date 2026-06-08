package com.mycompany.ebookshop.utils;

import com.mycompany.ebookshop.models.Book;
import com.mycompany.ebookshop.models.CartItem;
import com.mycompany.ebookshop.models.User;
import com.mycompany.ebookshop.database.CartDAO;
import java.util.ArrayList;
import java.util.List;

public class SessionManager {
    private static SessionManager instance;
    private final CartDAO cartDAO;
    private User currentUser;
    private List<CartItem> cart;
    private List<CartItem> checkoutItems;
    
    private SessionManager() {
        cartDAO = new CartDAO();
        cart = new ArrayList<>();
        checkoutItems = new ArrayList<>();
    }
    
    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }
    
    public User getCurrentUser() { return currentUser; }
    public void setCurrentUser(User user) {
        this.currentUser = user;
        if (user == null) {
            cart.clear();
            checkoutItems.clear();
            return;
        }
        loadCartForCurrentUser();
    }

    public void loadCartForCurrentUser() {
        cart.clear();
        if (currentUser == null || currentUser.getId() <= 0) {
            return;
        }
        cart.addAll(cartDAO.getCartItemsByUser(currentUser.getId()));
    }
    
    public List<CartItem> getCart() { return cart; }

    public List<CartItem> getCheckoutItems() { return checkoutItems; }

    public void setCheckoutItems(List<CartItem> items) {
        checkoutItems = items == null ? new ArrayList<>() : new ArrayList<>(items);
    }

    public void clearCheckoutItems() {
        checkoutItems.clear();
    }
    
    public void addToCart(Book book) {
        for (CartItem item : cart) {
            if (item.getBook().getId() == book.getId()) {
                item.setQuantity(item.getQuantity() + 1);
                syncCartItem(item);
                return;
            }
        }
        CartItem newItem = new CartItem(book, 1);
        cart.add(newItem);
        syncCartItem(newItem);
    }
    
    public void removeFromCart(CartItem item) {
        if (item != null && currentUser != null && currentUser.getId() > 0 && item.getBook() != null) {
            cartDAO.removeCartItem(currentUser.getId(), item.getBook().getId());
        }
        cart.remove(item);
    }
    
    public void updateCart() {
        if (currentUser == null || currentUser.getId() <= 0) {
            return;
        }

        for (CartItem item : cart) {
            syncCartItem(item);
        }
    }
    
    public void clearCart() {
        if (currentUser != null && currentUser.getId() > 0) {
            cartDAO.clearCart(currentUser.getId());
        }
        cart.clear();
    }
    
    public double getCartTotal() {
        return cart.stream().mapToDouble(CartItem::getTotal).sum();
    }
    
    public void clearSession() {
        currentUser = null;
        cart.clear();
        checkoutItems.clear();
    }

    private void syncCartItem(CartItem item) {
        if (currentUser == null || currentUser.getId() <= 0 || item == null || item.getBook() == null) {
            return;
        }
        cartDAO.upsertCartItem(currentUser.getId(), item.getBook().getId(), item.getQuantity());
    }
}